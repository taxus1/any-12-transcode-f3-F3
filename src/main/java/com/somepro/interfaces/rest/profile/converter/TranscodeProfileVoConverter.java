package com.somepro.interfaces.rest.profile.converter;

import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.profile.vo.TranscodeProfileVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TranscodeProfile（领域）→ TranscodeProfileVO（对外）转换器（用户接口层）。
 *
 * 接口层是唯一做领域对象 → VO 转换的地方：Controller 不许直接把领域对象塞进 Result 返回，
 * 否则 delFlag / createBy / updateBy 等内部字段会被无意识序列化出去。
 */
public final class TranscodeProfileVoConverter {

    private TranscodeProfileVoConverter() {
    }

    public static TranscodeProfileVO toVo(TranscodeProfile domain) {
        return new TranscodeProfileVO(
                domain.getId(),
                domain.getProfileCode(),
                domain.getProfileName(),
                domain.getMediaType() == null ? null : domain.getMediaType().name(),
                domain.getTargetFormat(),
                domain.getWidth(),
                domain.getHeight(),
                domain.getVideoBitrateKbps(),
                domain.getAudioBitrateKbps(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PageVO<TranscodeProfileVO> toPageVo(PageResult<TranscodeProfile> page) {
        List<TranscodeProfileVO> content = page.content().stream()
                .map(TranscodeProfileVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
