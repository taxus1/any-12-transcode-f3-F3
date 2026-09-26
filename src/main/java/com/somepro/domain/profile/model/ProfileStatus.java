package com.somepro.domain.profile.model;

import com.somepro.common.exception.BizException;

/**
 * 档位状态（领域枚举）：ENABLED 启用 / DISABLED 停用。
 */
public enum ProfileStatus {

    ENABLED, DISABLED;

    /** 按名字解析（大小写不敏感）；非法值抛业务异常。 */
    public static ProfileStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("档位状态不能为空（ENABLED/DISABLED）");
        }
        try {
            return ProfileStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("档位状态只支持 ENABLED/DISABLED：" + value);
        }
    }
}
