package com.somepro.domain.profile.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 转码档位聚合根（profile 上下文）：一个转码目标规格（格式、分辨率、码率）。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 TranscodeProfilePO）。
 *
 * 不变量：
 * - 档位编号 profileCode 必填且全局唯一（唯一性由应用层查重 + 数据库 uk_profile_code 兜底）；
 * - 档位名字、适用媒体类型、目标格式必填；
 * - 音频档位没有画面：宽 / 高 / 视频码率三栏一律置空（见 {@link #normalizeForMediaType()}）；
 * - 新登记的档位状态一律 ENABLED（启用）。
 */
@Getter
@Setter
public class TranscodeProfile extends BaseEntity {

    private Long id;

    /** 档位编号，全局唯一（形如 PF-001）。 */
    private String profileCode;

    /** 档位名字。 */
    private String profileName;

    /** 适用媒体类型：AUDIO / VIDEO。 */
    private ProfileMediaType mediaType;

    /** 目标格式（音频常见 mp3/wav，视频常见 mp4）。 */
    private String targetFormat;

    /** 目标宽度（音频档位为空）。 */
    private Integer width;

    /** 目标高度（音频档位为空）。 */
    private Integer height;

    /** 目标视频码率 kbps（音频档位为空）。 */
    private Integer videoBitrateKbps;

    /** 目标音频码率 kbps。 */
    private Integer audioBitrateKbps;

    private ProfileStatus status;

    /** 工厂方法：登记档位。新档位一律 ENABLED（启用），并保证初始不变量。 */
    public static TranscodeProfile register(String profileCode, String profileName, String mediaType,
                                            String targetFormat, Integer width, Integer height,
                                            Integer videoBitrateKbps, Integer audioBitrateKbps) {
        TranscodeProfile profile = new TranscodeProfile();
        profile.setProfileCode(profileCode);
        profile.setProfileName(profileName);
        profile.setMediaType(ProfileMediaType.of(mediaType));
        profile.setTargetFormat(targetFormat);
        profile.setWidth(width);
        profile.setHeight(height);
        profile.setVideoBitrateKbps(videoBitrateKbps);
        profile.setAudioBitrateKbps(audioBitrateKbps);
        profile.setStatus(ProfileStatus.ENABLED);
        profile.normalizeForMediaType();
        profile.validate();
        return profile;
    }

    /**
     * 领域行为：修改档位规格（编号允许改，改后的查重由应用层负责）。
     * status 传 null/空白表示不动当前状态。
     */
    public void revise(String profileCode, String profileName, String mediaType, String targetFormat,
                       Integer width, Integer height, Integer videoBitrateKbps, Integer audioBitrateKbps,
                       String status) {
        this.profileCode = profileCode;
        this.profileName = profileName;
        this.mediaType = ProfileMediaType.of(mediaType);
        this.targetFormat = targetFormat;
        this.width = width;
        this.height = height;
        this.videoBitrateKbps = videoBitrateKbps;
        this.audioBitrateKbps = audioBitrateKbps;
        if (status != null && !status.isBlank()) {
            this.status = ProfileStatus.of(status);
        }
        normalizeForMediaType();
        validate();
    }

    /** 音频档位没有画面：宽 / 高 / 视频码率一律置空，即使调用方误传也不落库。 */
    private void normalizeForMediaType() {
        if (mediaType == ProfileMediaType.AUDIO) {
            this.width = null;
            this.height = null;
            this.videoBitrateKbps = null;
        }
    }

    /** 聚合不变量：任何进入/离开领域的状态都要过这道校验。 */
    private void validate() {
        if (profileCode == null || profileCode.isBlank()) {
            throw new BizException("档位编号不能为空");
        }
        this.profileCode = profileCode.trim();
        if (profileName == null || profileName.isBlank()) {
            throw new BizException("档位名字不能为空");
        }
        this.profileName = profileName.trim();
        if (mediaType == null) {
            throw new BizException("适用媒体类型不能为空（AUDIO/VIDEO）");
        }
        if (targetFormat == null || targetFormat.isBlank()) {
            throw new BizException("目标格式不能为空");
        }
        this.targetFormat = targetFormat.trim().toLowerCase();
        if (width != null && width <= 0) {
            throw new BizException("目标宽度必须为正数");
        }
        if (height != null && height <= 0) {
            throw new BizException("目标高度必须为正数");
        }
        if (videoBitrateKbps != null && videoBitrateKbps <= 0) {
            throw new BizException("目标视频码率必须为正数");
        }
        if (audioBitrateKbps != null && audioBitrateKbps <= 0) {
            throw new BizException("目标音频码率必须为正数");
        }
    }
}
