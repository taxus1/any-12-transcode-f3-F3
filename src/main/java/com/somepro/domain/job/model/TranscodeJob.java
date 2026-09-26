package com.somepro.domain.job.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.media.model.AssetStatus;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.profile.model.ProfileStatus;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 转码任务聚合根（job 上下文）：一单对应「一个素材按一个档位转一次」。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 TranscodeJobPO）。
 *
 * 不变量：
 * - 任务编号 jobNo 必填且全局唯一（编号由应用层的发号器生成，唯一性由数据库 uk_job_no 兜底）；
 * - 提交时素材必须可转码（已完成 DONE / 已停用 DISABLED 的素材不能再提）、档位必须启用；
 * - 素材类型要与档位适用类型匹配（音频素材不能按视频档位转；图片素材因此没有可提的档位）；
 * - 新提交的任务一律 PENDING（待处理），等着节点来领；
 * - 只有 PENDING 能撤销，撤销必须写清原因；跑起来的、已出结果的都不能再撤。
 */
@Getter
@Setter
public class TranscodeJob extends BaseEntity {

    /** 优先级缺省值：提交时不填按 5 处理。 */
    public static final int DEFAULT_PRIORITY = 5;

    /** 优先级取值下限（1 最优先）。 */
    public static final int MIN_PRIORITY = 1;

    /** 优先级取值上限。 */
    public static final int MAX_PRIORITY = 10;

    /** 默认最多尝试次数。 */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    private Long id;

    /** 任务编号，全局唯一（形如 TJ-2026-0001），由应用层发号器生成。 */
    private String jobNo;

    /** 素材 id（t_media_asset.id）。 */
    private Long assetId;

    /** 档位 id（t_transcode_profile.id）。 */
    private Long profileId;

    /** 归属部门：配额与台账按它归集。 */
    private String ownerDept;

    /** 优先级，越小越先做。 */
    private Integer priority;

    private JobStatus status;

    /** 已尝试次数。 */
    private Integer attemptCount;

    /** 最多尝试次数。 */
    private Integer maxAttempts;

    /** 进度 0-100。 */
    private Integer progress;

    /** 产出文件路径。 */
    private String outputPath;

    /** 最近一次失败原因。 */
    private String errorMsg;

    /** 撤销原因（撤销时必填）。 */
    private String cancelReason;

    /** 提交时刻。 */
    private LocalDateTime submittedAt;

    /** 开始处理时刻。 */
    private LocalDateTime startedAt;

    /** 结束时刻（成功 / 失败 / 撤销都算结束）。 */
    private LocalDateTime finishedAt;

    /** 审核结果（PASS / REJECT），审核流程回写。 */
    private String reviewResult;

    /** 审核意见。 */
    private String reviewComment;

    /** 审核人。 */
    private String reviewBy;

    /** 审核时刻。 */
    private LocalDateTime reviewTime;

    /**
     * 工厂方法：提交任务。新任务一律 PENDING（待处理），并保证初始不变量。
     *
     * 跨聚合的提交门槛也守在这里：素材要可转码、档位要启用、素材类型要配得上档位适用类型；
     * 「同素材同档位不许重复在途」的查重需要读库，由应用层在调本方法前完成。
     */
    public static TranscodeJob submit(String jobNo, MediaAsset asset, TranscodeProfile profile,
                                      String ownerDept, Integer priority) {
        if (asset.getStatus() == AssetStatus.DONE) {
            throw new BizException("素材已完成转码，不能再提任务：" + asset.getAssetCode());
        }
        if (asset.getStatus() == AssetStatus.DISABLED) {
            throw new BizException("素材已停用，不能再提任务：" + asset.getAssetCode());
        }
        if (profile.getStatus() != ProfileStatus.ENABLED) {
            throw new BizException("转码档位已停用，不能提交任务：" + profile.getProfileCode());
        }
        if (!asset.getMediaType().name().equals(profile.getMediaType().name())) {
            throw new BizException("素材类型（" + asset.getMediaType().name()
                    + "）与档位适用类型（" + profile.getMediaType().name() + "）不匹配");
        }
        TranscodeJob job = new TranscodeJob();
        job.setJobNo(jobNo);
        job.setAssetId(asset.getId());
        job.setProfileId(profile.getId());
        job.setOwnerDept(ownerDept);
        job.setPriority(priority == null ? DEFAULT_PRIORITY : priority);
        job.setStatus(JobStatus.PENDING);
        job.setAttemptCount(0);
        job.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        job.setProgress(0);
        job.setSubmittedAt(LocalDateTime.now());
        job.validate();
        return job;
    }

    /**
     * 领域行为：撤销任务。只有还压在待处理里的单能撤，撤销原因必须写清楚；
     * 已经被节点跑起来的、已经出结果的（成功/失败）、已撤销的，都不能再撤。
     */
    public void cancel(String reason) {
        if (status != JobStatus.PENDING) {
            throw new BizException("只有待处理（PENDING）的任务才能撤销，当前状态：" + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException("撤销原因不能为空");
        }
        this.status = JobStatus.CANCELLED;
        this.cancelReason = reason.trim();
        this.finishedAt = LocalDateTime.now();
    }

    /** 聚合不变量：任何进入/离开领域的状态都要过这道校验。 */
    private void validate() {
        if (jobNo == null || jobNo.isBlank()) {
            throw new BizException("任务编号不能为空");
        }
        this.jobNo = jobNo.trim();
        if (assetId == null) {
            throw new BizException("素材不能为空");
        }
        if (profileId == null) {
            throw new BizException("转码档位不能为空");
        }
        if (ownerDept == null || ownerDept.isBlank()) {
            throw new BizException("归属部门不能为空");
        }
        this.ownerDept = ownerDept.trim();
        if (priority == null || priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new BizException("优先级需在 " + MIN_PRIORITY + "-" + MAX_PRIORITY
                    + " 之间（越小越先做），不填默认 " + DEFAULT_PRIORITY);
        }
    }
}
