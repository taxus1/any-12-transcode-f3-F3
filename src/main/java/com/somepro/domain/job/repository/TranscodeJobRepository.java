package com.somepro.domain.job.repository;

import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 转码任务聚合的仓储端口：由领域层定义，基础设施层实现（端口-适配器）。
 *
 * 分页条件全部可空：一个条件都不填时就是全量分页（表里早先录的数据也要能翻出来）。
 */
public interface TranscodeJobRepository {

    Mono<TranscodeJob> save(TranscodeJob job);

    Mono<TranscodeJob> findById(Long id);

    /** 同一素材 + 同一档位是否已有没跑完的单（PENDING / RUNNING）；有则不允许重复提交。 */
    Mono<Boolean> existsUnfinished(Long assetId, Long profileId);

    Mono<PageResult<TranscodeJob>> page(int pageNum, int pageSize, String jobNo, Long assetId,
                                        Long profileId, String ownerDept, String status);
}
