package com.somepro.infrastructure.persistence.job;

import com.somepro.application.job.port.JobNoPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Year;
import java.time.ZoneId;
import java.util.function.Supplier;

/**
 * 任务编号发号器（基础设施层）：按年分段、段内递增，形如 TJ-2026-0001。
 *
 * 实现口径：从库里读出本年前缀下已用过的最大序号，+1 作为下一个号。
 * - 并发下两个请求可能读到同一个最大值、拿到同一个号 —— 由数据库 uk_job_no 唯一索引兜底，
 *   撞号的一方 insert 失败，应用层会重取编号再试（见 TranscodeJobAppService#submit）；
 * - 年份按 Asia/Shanghai 取（与建表约定的时区一致），跨年自动另起一段从 0001 排。
 */
@Component
public class JobNoGenerator implements JobNoPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final String PREFIX = "TJ-";

    private final TranscodeJobMapper transcodeJobMapper;

    public JobNoGenerator(TranscodeJobMapper transcodeJobMapper) {
        this.transcodeJobMapper = transcodeJobMapper;
    }

    @Override
    public Mono<String> nextJobNo() {
        return blocking(() -> {
            String yearPrefix = PREFIX + Year.now(ZONE) + "-";
            Long maxSequence = transcodeJobMapper.findMaxSequence(yearPrefix);
            long next = (maxSequence == null ? 0 : maxSequence) + 1;
            return yearPrefix + String.format("%04d", next);
        });
    }

    /** 阻塞 DB 调用 → 响应式链路的桥接器（约定同 TranscodeJobRepositoryImpl#blocking）。 */
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
