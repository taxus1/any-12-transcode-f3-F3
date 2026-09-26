package com.somepro.application.job.port;

import reactor.core.publisher.Mono;

/**
 * 任务编号发号器端口：由应用层定义，基础设施层实现（端口-适配器）。
 *
 * 编号形如 TJ-2026-0001：按年分段、段内递增，全局不撞号
 * （最终一致性由数据库 uk_job_no 唯一索引兜底，撞号时应用层会重取编号再试）。
 */
public interface JobNoPort {

    /** 取下一个任务编号。 */
    Mono<String> nextJobNo();
}
