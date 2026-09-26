package com.somepro.domain.profile.model;

import com.somepro.common.exception.BizException;

/**
 * 档位适用媒体类型（领域枚举）：AUDIO 音频 / VIDEO 视频。
 *
 * 与素材的 MediaType 刻意分开：素材还有 IMAGE，而转码档位只服务音视频，
 * 各管各的取值范围，档位不会被误填成 IMAGE。
 */
public enum ProfileMediaType {

    AUDIO, VIDEO;

    /** 按名字解析（大小写不敏感）；空白或非法值抛业务异常。 */
    public static ProfileMediaType of(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("适用媒体类型不能为空（AUDIO/VIDEO）");
        }
        try {
            return ProfileMediaType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("适用媒体类型只支持 AUDIO/VIDEO：" + value);
        }
    }
}
