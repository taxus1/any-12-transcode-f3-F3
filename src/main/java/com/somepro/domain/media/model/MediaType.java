package com.somepro.domain.media.model;

import com.somepro.common.exception.BizException;

/**
 * 媒体类型（领域枚举）：AUDIO 音频 / VIDEO 视频 / IMAGE 图片。
 *
 * 素材三选一；转码档位只用其中的 AUDIO / VIDEO（见 profile 上下文的 ProfileMediaType，
 * 两边取值范围不同，刻意不共用一个枚举，避免档位被填成 IMAGE）。
 */
public enum MediaType {

    AUDIO, VIDEO, IMAGE;

    /** 按名字解析（大小写不敏感）；空白或非法值抛业务异常，挡住脏数据落库。 */
    public static MediaType of(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("媒体类型不能为空（AUDIO/VIDEO/IMAGE）");
        }
        try {
            return MediaType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("媒体类型只支持 AUDIO/VIDEO/IMAGE：" + value);
        }
    }
}
