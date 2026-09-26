package com.somepro.domain.media.model;

import com.somepro.common.exception.BizException;

/**
 * 素材状态（领域枚举）：READY 可转码 / TRANSCODING 转码中 / DONE 已完成 / DISABLED 已停用。
 *
 * 新登记的素材一律 READY（见 MediaAsset.register），后续状态由转码流程推进。
 */
public enum AssetStatus {

    READY, TRANSCODING, DONE, DISABLED;

    /** 按名字解析（大小写不敏感）；非法值抛业务异常。 */
    public static AssetStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("素材状态不能为空（READY/TRANSCODING/DONE/DISABLED）");
        }
        try {
            return AssetStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("素材状态只支持 READY/TRANSCODING/DONE/DISABLED：" + value);
        }
    }
}
