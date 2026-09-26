package com.somepro.domain.job.model;

import com.somepro.common.exception.BizException;

/**
 * 任务状态（领域枚举）：PENDING 待处理 / RUNNING 处理中 / SUCCESS 成功 / FAILED 失败 / CANCELLED 已取消。
 *
 * 生命周期：提交即 PENDING（等着节点来领）→ 节点领走变 RUNNING → 跑出结果 SUCCESS / FAILED；
 * 还没被领走的 PENDING 单可以撤销成 CANCELLED，跑起来的和已出结果的都不能再撤。
 */
public enum JobStatus {

    PENDING, RUNNING, SUCCESS, FAILED, CANCELLED;

    /** 按名字解析（大小写不敏感）；非法值抛业务异常。 */
    public static JobStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("任务状态不能为空（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）");
        }
        try {
            return JobStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("任务状态只支持 PENDING/RUNNING/SUCCESS/FAILED/CANCELLED：" + value);
        }
    }

    /** 还没跑完：压在待处理里，或者正被节点跑着。重复提交拦截只看这两个状态。 */
    public boolean isUnfinished() {
        return this == PENDING || this == RUNNING;
    }
}
