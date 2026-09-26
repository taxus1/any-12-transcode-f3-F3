package com.somepro.application.job;

import com.somepro.application.job.port.JobNoPort;
import com.somepro.common.exception.BizException;
import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.domain.job.repository.TranscodeJobRepository;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.repository.MediaAssetRepository;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.profile.repository.TranscodeProfileRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * 转码任务用例编排（应用层）：提交 / 撤销 / 查看 / 分页。
 *
 * 不写业务规则（规则在领域层 TranscodeJob），只做编排：
 * - 提交：先把素材、档位两个聚合取出来（不存在直接报错），再查「同素材同档位是否有未跑完的单」，
 *   然后经发号器取号、走领域工厂建单、落库；
 * - 任务编号唯一性：发号器按年递增发号，数据库 uk_job_no 唯一索引兜底 —— 并发下两个请求
 *   可能拿到同一个号，后到的 insert 撞索引，这里重取编号再试，仍撞才翻成业务异常；
 * - 出入参都是领域对象，不认识 PO、也不认识 VO。
 */
@Service
public class TranscodeJobAppService {

    private final TranscodeJobRepository transcodeJobRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final TranscodeProfileRepository transcodeProfileRepository;
    private final JobNoPort jobNoPort;

    public TranscodeJobAppService(TranscodeJobRepository transcodeJobRepository,
                                  MediaAssetRepository mediaAssetRepository,
                                  TranscodeProfileRepository transcodeProfileRepository,
                                  JobNoPort jobNoPort) {
        this.transcodeJobRepository = transcodeJobRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.transcodeProfileRepository = transcodeProfileRepository;
        this.jobNoPort = jobNoPort;
    }

    /** 提交任务：一个素材按一个档位转一次，提出来是待处理 PENDING，等着节点来领。 */
    public Mono<TranscodeJob> submit(Long assetId, Long profileId, String ownerDept, Integer priority) {
        return Mono.defer(() -> {
                    Mono<MediaAsset> assetMono = mediaAssetRepository.findById(assetId)
                            .switchIfEmpty(Mono.error(new BizException("素材不存在：" + assetId)));
                    Mono<TranscodeProfile> profileMono = transcodeProfileRepository.findById(profileId)
                            .switchIfEmpty(Mono.error(new BizException("转码档位不存在：" + profileId)));
                    return Mono.zip(assetMono, profileMono)
                            .flatMap(pair -> transcodeJobRepository.existsUnfinished(assetId, profileId)
                                    .flatMap(unfinished -> unfinished
                                            ? Mono.<String>error(new BizException(
                                                    "该素材按此档位已有一单未跑完（待处理或处理中），请勿重复提交"))
                                            : jobNoPort.nextJobNo())
                                    .map(jobNo -> TranscodeJob.submit(jobNo, pair.getT1(), pair.getT2(),
                                            ownerDept, priority)))
                            .flatMap(transcodeJobRepository::save);
                })
                // 并发撞号（uk_job_no）时整条链路重试：defer 里会重新取号，最多再试 2 次
                .retryWhen(Retry.max(2).filter(DuplicateKeyException.class::isInstance))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BizException("任务编号冲突，请重新提交"));
    }

    /** 撤销任务：先取出聚合，走领域行为改状态（非法状态 / 空原因在领域层抛），再落库。 */
    public Mono<TranscodeJob> cancel(Long id, String reason) {
        return transcodeJobRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("任务不存在：" + id)))
                .flatMap(job -> {
                    job.cancel(reason);
                    return transcodeJobRepository.save(job);
                });
    }

    public Mono<TranscodeJob> get(Long id) {
        return transcodeJobRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("任务不存在：" + id)));
    }

    /** 分页翻查：条件都可空，一个都不填就是全量分页。 */
    public Mono<PageResult<TranscodeJob>> page(int pageNum, int pageSize, String jobNo, Long assetId,
                                               Long profileId, String ownerDept, String status) {
        return transcodeJobRepository.page(pageNum, pageSize, jobNo, assetId, profileId,
                ownerDept, normalize(status));
    }

    /** 枚举类查询条件统一转大写，调用方传小写也能查到。 */
    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toUpperCase();
    }
}
