package com.somepro.infrastructure.persistence.media.converter;

import com.somepro.domain.media.model.AssetStatus;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.model.MediaType;
import com.somepro.infrastructure.persistence.media.po.MediaAssetPO;

/**
 * MediaAssetPO（表）↔ MediaAsset（领域）转换器（基础设施层）。
 *
 * 这是 PO 与领域模型之间**唯一**的转换入口：仓储适配器进去转 PO 落库、出来转回领域对象，
 * 领域层和接口层都不应看到 MediaAssetPO。
 *
 * 枚举 ↔ VARCHAR 在这里互转；审计字段与 delFlag 也一并搬运（同 DemoItemPoConverter 的约定）。
 */
public final class MediaAssetPoConverter {

    private MediaAssetPoConverter() {
    }

    public static MediaAssetPO toPo(MediaAsset domain) {
        MediaAssetPO po = new MediaAssetPO();
        po.setId(domain.getId());
        po.setAssetCode(domain.getAssetCode());
        po.setFileName(domain.getFileName());
        po.setMediaType(domain.getMediaType() == null ? null : domain.getMediaType().name());
        po.setFileExt(domain.getFileExt());
        po.setSizeBytes(domain.getSizeBytes());
        po.setDurationMs(domain.getDurationMs());
        po.setChecksum(domain.getChecksum());
        po.setOwnerDept(domain.getOwnerDept());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static MediaAsset toDomain(MediaAssetPO po) {
        MediaAsset domain = new MediaAsset();
        domain.setId(po.getId());
        domain.setAssetCode(po.getAssetCode());
        domain.setFileName(po.getFileName());
        domain.setMediaType(po.getMediaType() == null ? null : MediaType.valueOf(po.getMediaType()));
        domain.setFileExt(po.getFileExt());
        domain.setSizeBytes(po.getSizeBytes());
        domain.setDurationMs(po.getDurationMs());
        domain.setChecksum(po.getChecksum());
        domain.setOwnerDept(po.getOwnerDept());
        domain.setStatus(po.getStatus() == null ? null : AssetStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
