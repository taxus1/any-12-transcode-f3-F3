package com.somepro.interfaces.rest.media.converter;

import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.media.vo.MediaAssetVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MediaAsset（领域）→ MediaAssetVO（对外）转换器（用户接口层）。
 *
 * 接口层是唯一做领域对象 → VO 转换的地方：Controller 不许直接把领域对象塞进 Result 返回，
 * 否则 delFlag / createBy / updateBy 等内部字段会被无意识序列化出去。
 */
public final class MediaAssetVoConverter {

    private MediaAssetVoConverter() {
    }

    public static MediaAssetVO toVo(MediaAsset domain) {
        return new MediaAssetVO(
                domain.getId(),
                domain.getAssetCode(),
                domain.getFileName(),
                domain.getMediaType() == null ? null : domain.getMediaType().name(),
                domain.getFileExt(),
                domain.getSizeBytes(),
                domain.getDurationMs(),
                domain.getChecksum(),
                domain.getOwnerDept(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PageVO<MediaAssetVO> toPageVo(PageResult<MediaAsset> page) {
        List<MediaAssetVO> content = page.content().stream()
                .map(MediaAssetVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
