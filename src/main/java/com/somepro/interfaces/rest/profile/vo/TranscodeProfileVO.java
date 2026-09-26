package com.somepro.interfaces.rest.profile.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 转码档位对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 只暴露允许外部看到的字段。刻意不含：
 * - delFlag：内部软删状态
 * - createBy / updateBy：内部审计人
 * - updateTime：内部维护时间
 * 这些字段留在领域对象与 PO 里，不进 API 契约 —— 改库表不会连带改接口。
 */
public record TranscodeProfileVO(Long id, String profileCode, String profileName, String mediaType,
                                 String targetFormat, Integer width, Integer height,
                                 Integer videoBitrateKbps, Integer audioBitrateKbps,
                                 String status, LocalDateTime createTime) implements Serializable {
}
