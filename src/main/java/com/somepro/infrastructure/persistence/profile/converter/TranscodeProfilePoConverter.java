package com.somepro.infrastructure.persistence.profile.converter;

import com.somepro.domain.profile.model.ProfileMediaType;
import com.somepro.domain.profile.model.ProfileStatus;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.infrastructure.persistence.profile.po.TranscodeProfilePO;

/**
 * TranscodeProfilePO（表）↔ TranscodeProfile（领域）转换器（基础设施层）。
 *
 * 这是 PO 与领域模型之间**唯一**的转换入口：仓储适配器进去转 PO 落库、出来转回领域对象，
 * 领域层和接口层都不应看到 TranscodeProfilePO。
 *
 * 枚举 ↔ VARCHAR 在这里互转；审计字段与 delFlag 也一并搬运（同 DemoItemPoConverter 的约定）。
 */
public final class TranscodeProfilePoConverter {

    private TranscodeProfilePoConverter() {
    }

    public static TranscodeProfilePO toPo(TranscodeProfile domain) {
        TranscodeProfilePO po = new TranscodeProfilePO();
        po.setId(domain.getId());
        po.setProfileCode(domain.getProfileCode());
        po.setProfileName(domain.getProfileName());
        po.setMediaType(domain.getMediaType() == null ? null : domain.getMediaType().name());
        po.setTargetFormat(domain.getTargetFormat());
        po.setWidth(domain.getWidth());
        po.setHeight(domain.getHeight());
        po.setVideoBitrateKbps(domain.getVideoBitrateKbps());
        po.setAudioBitrateKbps(domain.getAudioBitrateKbps());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static TranscodeProfile toDomain(TranscodeProfilePO po) {
        TranscodeProfile domain = new TranscodeProfile();
        domain.setId(po.getId());
        domain.setProfileCode(po.getProfileCode());
        domain.setProfileName(po.getProfileName());
        domain.setMediaType(po.getMediaType() == null ? null : ProfileMediaType.valueOf(po.getMediaType()));
        domain.setTargetFormat(po.getTargetFormat());
        domain.setWidth(po.getWidth());
        domain.setHeight(po.getHeight());
        domain.setVideoBitrateKbps(po.getVideoBitrateKbps());
        domain.setAudioBitrateKbps(po.getAudioBitrateKbps());
        domain.setStatus(po.getStatus() == null ? null : ProfileStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
