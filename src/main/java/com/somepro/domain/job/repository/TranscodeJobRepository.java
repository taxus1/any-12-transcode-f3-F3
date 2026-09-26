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

    /**
     * 落库一条新任务（提交用例专用）：
     * - 在事务里锁住素材行（FOR UPDATE），锁内复查「同素材同档位无未完成任务」，
     *   挡住并发重复提交；
     * - 分配任务编号（TJ-年份-四位序号，按年递增），uk_job_no 唯一索引兜底，撞号自动重试。
     */
    Mono<TranscodeJob> submitNew(TranscodeJob job);

    Mono<TranscodeJob> findById(Long id);

    /** 同素材同档位是否已有未完成任务（PENDING/RUNNING）。 */
    Mono<Boolean> existsActiveByAssetAndProfile(Long assetId, Long profileId);

    Mono<PageResult<TranscodeJob>> page(int pageNum, int pageSize, String jobNo, String status,
                                        String ownerDept, Long assetId, Long profileId);

    /**
     * 撤销落库（乐观条件更新）：仅当库里仍是 PENDING 才更新为 CANCELLED，
     * 防止「查出来是待处理 → 节点同时领走 → 又被撤销」的并发窗口。
     */
    Mono<TranscodeJob> cancelIfPending(TranscodeJob job);
}
