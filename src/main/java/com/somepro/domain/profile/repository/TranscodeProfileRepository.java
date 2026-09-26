package com.somepro.domain.profile.repository;

import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 转码档位聚合的仓储端口：由领域层定义，基础设施层实现（端口-适配器）。
 *
 * 分页条件全部可空：一个条件都不填时就是全量分页（表里早先录的数据也要能翻出来）。
 */
public interface TranscodeProfileRepository {

    Mono<TranscodeProfile> save(TranscodeProfile profile);

    Mono<TranscodeProfile> findById(Long id);

    /** 按档位编号查（编号全局唯一）；查不到返回空 Mono。 */
    Mono<TranscodeProfile> findByProfileCode(String profileCode);

    Mono<PageResult<TranscodeProfile>> page(int pageNum, int pageSize, String profileCode,
                                            String profileName, String mediaType, String status);

    Mono<Void> softDelete(Long id);
}
