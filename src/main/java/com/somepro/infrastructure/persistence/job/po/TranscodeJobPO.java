package com.somepro.infrastructure.persistence.job.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_transcode_job 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：字段与列一一对应，不放任何业务规则（规则在领域对象 TranscodeJob）。
 * status 在表里是 VARCHAR，这里用 String；与领域枚举的互转见 TranscodeJobPoConverter。
 *
 * ID 策略 IdType.INPUT：由应用层用雪花算法分配后传入，与仓储适配器里的 IdUtil 一致。
 */
@Getter
@Setter
@TableName("t_transcode_job")
public class TranscodeJobPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("job_no")
    private String jobNo;

    @TableField("asset_id")
    private Long assetId;

    @TableField("profile_id")
    private Long profileId;

    @TableField("owner_dept")
    private String ownerDept;

    @TableField("priority")
    private Integer priority;

    @TableField("status")
    private String status;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("max_attempts")
    private Integer maxAttempts;

    @TableField("progress")
    private Integer progress;

    @TableField("output_path")
    private String outputPath;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("review_result")
    private String reviewResult;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("review_by")
    private String reviewBy;

    @TableField("review_time")
    private LocalDateTime reviewTime;
}
