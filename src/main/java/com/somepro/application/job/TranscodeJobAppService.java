package com.somepro.application.job;

import com.somepro.common.exception.BizException;
import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.domain.job.repository.TranscodeJobRepository;
import com.somepro.domain.media.model.AssetStatus;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.repository.MediaAssetRepository;
import com.somepro.domain.profile.model.ProfileStatus;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.profile.repository.TranscodeProfileRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 转码任务用例编排（应用层）：提交 / 撤销 / 查看 / 分页。
 *
 * 不写业务规则（规则在领域层 TranscodeJob），只做编排：
 * - 提交前校验素材与档位状态、归属部门一致性、类型匹配、无同档位未完成任务；
 * - 并发重复提交与任务编号分配由仓储在事务里兜底（见 TranscodeJobRepository.submitNew）；
 * - 出入参都是领域对象，不认识 PO、也不认识 VO。
 */
@Service
public class TranscodeJobAppService {

    private static final String DUPLICATE_MSG = "该素材在此档位下已有未完成的转码任务（待处理或处理中），请勿重复提交";

    private final TranscodeJobRepository transcodeJobRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final TranscodeProfileRepository transcodeProfileRepository;

    public TranscodeJobAppService(TranscodeJobRepository transcodeJobRepository,
                                  MediaAssetRepository mediaAssetRepository,
                                  TranscodeProfileRepository transcodeProfileRepository) {
        this.transcodeJobRepository = transcodeJobRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.transcodeProfileRepository = transcodeProfileRepository;
    }

    /**
     * 提交转码任务：一个素材按一个档位转一次，提出来是 PENDING。
     *
     * 校验顺序：素材存在且可转码 → 归属部门与素材一致 → 档位存在且启用 →
     * 档位适用类型与素材类型匹配 → 同素材同档位无未完成任务。
     */
    public Mono<TranscodeJob> submit(Long assetId, Long profileId, String ownerDept, Integer priority) {
        return mediaAssetRepository.findById(assetId)
                .switchIfEmpty(Mono.error(new BizException("素材不存在：" + assetId)))
                .flatMap(asset -> {
                    requireAssetSubmittable(asset, ownerDept);
                    return transcodeProfileRepository.findById(profileId)
                            .switchIfEmpty(Mono.error(new BizException("转码档位不存在：" + profileId)))
                            .flatMap(profile -> {
                                requireProfileUsable(profile, asset);
                                return transcodeJobRepository.existsActiveByAssetAndProfile(assetId, profileId)
                                        .flatMap(exists -> {
                                            if (exists) {
                                                return Mono.<TranscodeJob>error(new BizException(DUPLICATE_MSG));
                                            }
                                            return transcodeJobRepository.submitNew(
                                                    TranscodeJob.submit(assetId, profileId, ownerDept, priority));
                                        });
                            });
                });
    }

    /** 撤销任务：只有 PENDING 可撤，且必须写明原因（规则在领域层，落库由仓储带条件兜底）。 */
    public Mono<TranscodeJob> cancel(Long id, String reason) {
        return transcodeJobRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("转码任务不存在：" + id)))
                .flatMap(job -> {
                    job.cancel(reason);
                    return transcodeJobRepository.cancelIfPending(job);
                });
    }

    public Mono<TranscodeJob> get(Long id) {
        return transcodeJobRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("转码任务不存在：" + id)));
    }

    /** 分页翻查：条件都可空，一个都不填就是全量分页。 */
    public Mono<PageResult<TranscodeJob>> page(int pageNum, int pageSize, String jobNo, String status,
                                               String ownerDept, Long assetId, Long profileId) {
        return transcodeJobRepository.page(pageNum, pageSize, jobNo, normalize(status),
                ownerDept, assetId, profileId);
    }

    /** 素材可提交校验：DONE（已完成）与 DISABLED（已停用）不能再提；归属部门必须与素材一致。 */
    private static void requireAssetSubmittable(MediaAsset asset, String ownerDept) {
        if (asset.getStatus() == AssetStatus.DONE) {
            throw new BizException("素材已完成转码，无需再提交：" + asset.getId());
        }
        if (asset.getStatus() == AssetStatus.DISABLED) {
            throw new BizException("素材已停用，不能提交转码：" + asset.getId());
        }
        if (ownerDept == null || ownerDept.isBlank()) {
            throw new BizException("归属部门不能为空");
        }
        if (!asset.getOwnerDept().equals(ownerDept.trim())) {
            throw new BizException("归属部门与素材不一致，素材归属部门：" + asset.getOwnerDept());
        }
    }

    /** 档位可用校验：必须启用，且适用媒体类型与素材一致（图片素材天然匹配不到任何档位）。 */
    private static void requireProfileUsable(TranscodeProfile profile, MediaAsset asset) {
        if (profile.getStatus() != ProfileStatus.ENABLED) {
            throw new BizException("转码档位已停用：" + profile.getId());
        }
        if (!profile.getMediaType().name().equals(asset.getMediaType().name())) {
            throw new BizException("档位适用类型（" + profile.getMediaType()
                    + "）与素材类型（" + asset.getMediaType() + "）不匹配");
        }
    }

    /** 枚举类查询条件统一转大写，调用方传小写也能查到。 */
    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toUpperCase();
    }
}
