package com.somepro.application.profile;

import com.somepro.common.exception.BizException;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.profile.repository.TranscodeProfileRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 转码档位用例编排（应用层）：登记 / 修改 / 查看 / 分页 / 删除。
 *
 * 不写业务规则（规则在领域层 TranscodeProfile），只做编排：
 * - 编号唯一性：先按编号查一次给出明确报错，数据库 uk_profile_code 唯一索引兜底
 *   （并发下两个相同编号同时登记时，后到的 insert 会撞索引，这里把它翻成业务异常）；
 * - 出入参都是领域对象，不认识 PO、也不认识 VO。
 */
@Service
public class TranscodeProfileAppService {

    private final TranscodeProfileRepository transcodeProfileRepository;

    public TranscodeProfileAppService(TranscodeProfileRepository transcodeProfileRepository) {
        this.transcodeProfileRepository = transcodeProfileRepository;
    }

    /** 登记档位：编号不能撞，新档位默认 ENABLED。 */
    public Mono<TranscodeProfile> register(String profileCode, String profileName, String mediaType,
                                           String targetFormat, Integer width, Integer height,
                                           Integer videoBitrateKbps, Integer audioBitrateKbps) {
        TranscodeProfile profile = TranscodeProfile.register(profileCode, profileName, mediaType,
                targetFormat, width, height, videoBitrateKbps, audioBitrateKbps);
        return transcodeProfileRepository.findByProfileCode(profile.getProfileCode())
                .flatMap(exists -> Mono.<TranscodeProfile>error(
                        new BizException("档位编号已存在：" + profile.getProfileCode())))
                .switchIfEmpty(Mono.defer(() -> transcodeProfileRepository.save(profile)))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BizException("档位编号已存在：" + profile.getProfileCode()));
    }

    /** 修改档位：先取出聚合，走领域行为改状态，再落库。编号变更时同样查重。 */
    public Mono<TranscodeProfile> update(Long id, String profileCode, String profileName,
                                         String mediaType, String targetFormat,
                                         Integer width, Integer height,
                                         Integer videoBitrateKbps, Integer audioBitrateKbps,
                                         String status) {
        return transcodeProfileRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("转码档位不存在：" + id)))
                .flatMap(profile -> {
                    String newCode = profileCode == null ? null : profileCode.trim();
                    if (newCode != null && !newCode.equals(profile.getProfileCode())) {
                        return transcodeProfileRepository.findByProfileCode(newCode)
                                .flatMap(dup -> Mono.<TranscodeProfile>error(
                                        new BizException("档位编号已存在：" + newCode)))
                                .switchIfEmpty(Mono.defer(() -> {
                                    profile.revise(profileCode, profileName, mediaType, targetFormat,
                                            width, height, videoBitrateKbps, audioBitrateKbps, status);
                                    return transcodeProfileRepository.save(profile);
                                }));
                    }
                    profile.revise(profileCode, profileName, mediaType, targetFormat,
                            width, height, videoBitrateKbps, audioBitrateKbps, status);
                    return transcodeProfileRepository.save(profile);
                })
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BizException("档位编号已存在：" + profileCode));
    }

    public Mono<TranscodeProfile> get(Long id) {
        return transcodeProfileRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("转码档位不存在：" + id)));
    }

    /** 分页翻查：条件都可空，一个都不填就是全量分页。 */
    public Mono<PageResult<TranscodeProfile>> page(int pageNum, int pageSize, String profileCode,
                                                   String profileName, String mediaType, String status) {
        return transcodeProfileRepository.page(pageNum, pageSize, profileCode, profileName,
                normalize(mediaType), normalize(status));
    }

    public Mono<Void> delete(Long id) {
        return transcodeProfileRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("转码档位不存在：" + id)))
                .flatMap(profile -> transcodeProfileRepository.softDelete(id));
    }

    /** 枚举类查询条件统一转大写，调用方传小写也能查到。 */
    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toUpperCase();
    }
}
