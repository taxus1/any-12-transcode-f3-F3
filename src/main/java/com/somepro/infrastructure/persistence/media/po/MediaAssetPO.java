package com.somepro.infrastructure.persistence.media.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_media_asset 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：字段与列一一对应，不放任何业务规则（规则在领域对象 MediaAsset）。
 * mediaType / status 在表里是 VARCHAR，这里用 String；与领域枚举的互转见 MediaAssetPoConverter。
 *
 * ID 策略 IdType.INPUT：由应用层用雪花算法分配后传入，与仓储适配器里的 IdUtil 一致。
 */
@Getter
@Setter
@TableName("t_media_asset")
public class MediaAssetPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("asset_code")
    private String assetCode;

    @TableField("file_name")
    private String fileName;

    @TableField("media_type")
    private String mediaType;

    @TableField("file_ext")
    private String fileExt;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("checksum")
    private String checksum;

    @TableField("owner_dept")
    private String ownerDept;

    @TableField("status")
    private String status;
}
