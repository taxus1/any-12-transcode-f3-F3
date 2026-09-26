package com.somepro.interfaces.rest.media.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 素材对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 只暴露允许外部看到的字段。刻意不含：
 * - delFlag：内部软删状态
 * - createBy / updateBy：内部审计人
 * - updateTime：内部维护时间
 * 这些字段留在领域对象与 PO 里，不进 API 契约 —— 改库表不会连带改接口。
 */
public record MediaAssetVO(Long id, String assetCode, String fileName, String mediaType,
                           String fileExt, Long sizeBytes, Long durationMs, String checksum,
                           String ownerDept, String status, LocalDateTime createTime)
        implements Serializable {
}
