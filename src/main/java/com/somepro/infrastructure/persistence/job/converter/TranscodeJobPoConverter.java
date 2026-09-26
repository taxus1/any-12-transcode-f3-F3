package com.somepro.infrastructure.persistence.job.converter;

import com.somepro.domain.job.model.JobStatus;
import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.infrastructure.persistence.job.po.TranscodeJobPO;

/**
 * TranscodeJobPO（表）↔ TranscodeJob（领域）转换器（基础设施层）。
 *
 * 这是 PO 与领域模型之间**唯一**的转换入口：仓储适配器进去转 PO 落库、出来转回领域对象，
 * 领域层和接口层都不应看到 TranscodeJobPO。
 *
 * 枚举 ↔ VARCHAR 在这里互转；审计字段与 delFlag 也一并搬运（同 DemoItemPoConverter 的约定）。
 */
public final class TranscodeJobPoConverter {

    private TranscodeJobPoConverter() {
    }

    public static TranscodeJobPO toPo(TranscodeJob domain) {
        TranscodeJobPO po = new TranscodeJobPO();
        po.setId(domain.getId());
        po.setJobNo(domain.getJobNo());
        po.setAssetId(domain.getAssetId());
        po.setProfileId(domain.getProfileId());
        po.setOwnerDept(domain.getOwnerDept());
        po.setPriority(domain.getPriority());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setAttemptCount(domain.getAttemptCount());
        po.setMaxAttempts(domain.getMaxAttempts());
        po.setProgress(domain.getProgress());
        po.setOutputPath(domain.getOutputPath());
        po.setErrorMsg(domain.getErrorMsg());
        po.setCancelReason(domain.getCancelReason());
        po.setSubmittedAt(domain.getSubmittedAt());
        po.setStartedAt(domain.getStartedAt());
        po.setFinishedAt(domain.getFinishedAt());
        po.setReviewResult(domain.getReviewResult());
        po.setReviewComment(domain.getReviewComment());
        po.setReviewBy(domain.getReviewBy());
        po.setReviewTime(domain.getReviewTime());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static TranscodeJob toDomain(TranscodeJobPO po) {
        TranscodeJob domain = new TranscodeJob();
        domain.setId(po.getId());
        domain.setJobNo(po.getJobNo());
        domain.setAssetId(po.getAssetId());
        domain.setProfileId(po.getProfileId());
        domain.setOwnerDept(po.getOwnerDept());
        domain.setPriority(po.getPriority());
        domain.setStatus(po.getStatus() == null ? null : JobStatus.valueOf(po.getStatus()));
        domain.setAttemptCount(po.getAttemptCount());
        domain.setMaxAttempts(po.getMaxAttempts());
        domain.setProgress(po.getProgress());
        domain.setOutputPath(po.getOutputPath());
        domain.setErrorMsg(po.getErrorMsg());
        domain.setCancelReason(po.getCancelReason());
        domain.setSubmittedAt(po.getSubmittedAt());
        domain.setStartedAt(po.getStartedAt());
        domain.setFinishedAt(po.getFinishedAt());
        domain.setReviewResult(po.getReviewResult());
        domain.setReviewComment(po.getReviewComment());
        domain.setReviewBy(po.getReviewBy());
        domain.setReviewTime(po.getReviewTime());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
