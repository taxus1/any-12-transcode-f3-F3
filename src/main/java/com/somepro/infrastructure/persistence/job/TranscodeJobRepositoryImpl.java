package com.somepro.infrastructure.persistence.job;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.job.model.JobStatus;
import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.domain.job.repository.TranscodeJobRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.job.converter.TranscodeJobPoConverter;
import com.somepro.infrastructure.persistence.job.po.TranscodeJobPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 转码任务仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * 约定同 DemoItemRepositoryImpl：
 * - 所有 DB 调用必须经 {@link #blocking} 桥接到 boundedElastic，严禁在 event-loop 上跑 JDBC；
 * - Mapper 只认 {@link TranscodeJobPO}，领域层只认 {@link TranscodeJob}，两者在本类里互转；
 * - 软删除交给 @TableLogic，不手写 del_flag 条件；
 * - 分页统一用 PageHelper.startPage()，finally 里必须 clearPage()。
 */
@Repository
public class TranscodeJobRepositoryImpl implements TranscodeJobRepository {

    private final TranscodeJobMapper transcodeJobMapper;

    public TranscodeJobRepositoryImpl(TranscodeJobMapper transcodeJobMapper) {
        this.transcodeJobMapper = transcodeJobMapper;
    }

    @Override
    public Mono<TranscodeJob> save(TranscodeJob job) {
        return blocking(() -> {
            TranscodeJobPO po = TranscodeJobPoConverter.toPo(job);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                transcodeJobMapper.insert(po);
            } else {
                transcodeJobMapper.updateById(po);
            }
            // insert 后框架会回填 id 与审计字段，转回领域对象一并返回
            return TranscodeJobPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<TranscodeJob> findById(Long id) {
        return blocking(() -> {
            TranscodeJobPO po = transcodeJobMapper.selectById(id);
            // 返回 null 时 Mono.fromCallable 会自动转成空信号
            return po == null ? null : TranscodeJobPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Boolean> existsUnfinished(Long assetId, Long profileId) {
        return blocking(() -> {
            // 没跑完 = 还压在待处理里，或者正被节点跑着；@TableLogic 自动带 del_flag = 0
            Long count = transcodeJobMapper.selectCount(Wrappers.<TranscodeJobPO>lambdaQuery()
                    .eq(TranscodeJobPO::getAssetId, assetId)
                    .eq(TranscodeJobPO::getProfileId, profileId)
                    .in(TranscodeJobPO::getStatus, JobStatus.PENDING.name(), JobStatus.RUNNING.name()));
            return count != null && count > 0;
        });
    }

    @Override
    public Mono<PageResult<TranscodeJob>> page(int pageNum, int pageSize, String jobNo, Long assetId,
                                               Long profileId, String ownerDept, String status) {
        return this.<PageResult<TranscodeJob>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                // 条件全部可空：一个都不填时不拼任何条件，全量分页（早先录的数据也能翻出来）
                LambdaQueryWrapper<TranscodeJobPO> wrapper = Wrappers.<TranscodeJobPO>lambdaQuery()
                        .eq(StrUtil.isNotBlank(jobNo), TranscodeJobPO::getJobNo, jobNo)
                        .eq(assetId != null, TranscodeJobPO::getAssetId, assetId)
                        .eq(profileId != null, TranscodeJobPO::getProfileId, profileId)
                        .eq(StrUtil.isNotBlank(ownerDept), TranscodeJobPO::getOwnerDept, ownerDept)
                        .eq(StrUtil.isNotBlank(status), TranscodeJobPO::getStatus, status)
                        .orderByDesc(TranscodeJobPO::getId);
                List<TranscodeJobPO> rows = transcodeJobMapper.selectList(wrapper);
                // 命中分页插件时返回的是 com.github.pagehelper.Page，可直接取总数
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<TranscodeJob> content = rows.stream()
                        .map(TranscodeJobPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // 分页插件靠 ThreadLocal 传递分页参数，必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    /**
     * 阻塞 DB 调用 → 响应式链路的桥接器。
     *
     * 1. 先在响应式线程上从 Reactor Context 取操作人（切线程后就取不到了）
     * 2. 再切到 boundedElastic 执行 JDBC
     * 3. 把操作人放进 AuditContextHolder，供 MetaObjectHandler 填充 createBy / updateBy
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
