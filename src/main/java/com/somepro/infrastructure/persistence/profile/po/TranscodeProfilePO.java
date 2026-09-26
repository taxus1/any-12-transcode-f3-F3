package com.somepro.infrastructure.persistence.profile.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_transcode_profile 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：字段与列一一对应，不放任何业务规则（规则在领域对象 TranscodeProfile）。
 * mediaType / status 在表里是 VARCHAR，这里用 String；与领域枚举的互转见 TranscodeProfilePoConverter。
 *
 * ID 策略 IdType.INPUT：由应用层用雪花算法分配后传入，与仓储适配器里的 IdUtil 一致。
 */
@Getter
@Setter
@TableName("t_transcode_profile")
public class TranscodeProfilePO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("profile_code")
    private String profileCode;

    @TableField("profile_name")
    private String profileName;

    @TableField("media_type")
    private String mediaType;

    @TableField("target_format")
    private String targetFormat;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField("video_bitrate_kbps")
    private Integer videoBitrateKbps;

    @TableField("audio_bitrate_kbps")
    private Integer audioBitrateKbps;

    @TableField("status")
    private String status;
}
