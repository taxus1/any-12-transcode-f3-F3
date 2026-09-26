package com.somepro.domain.media.repository;

import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 素材聚合的仓储端口：由领域层定义，基础设施层实现（端口-适配器）。
 *
 * 分页条件全部可空：一个条件都不填时就是全量分页（表里早先录的数据也要能翻出来）。
 */
public interface MediaAssetRepository {

    Mono<MediaAsset> save(MediaAsset asset);

    Mono<MediaAsset> findById(Long id);

    /** 按素材编号查（编号全局唯一）；查不到返回空 Mono。 */
    Mono<MediaAsset> findByAssetCode(String assetCode);

    Mono<PageResult<MediaAsset>> page(int pageNum, int pageSize, String assetCode, String fileName,
                                      String mediaType, String ownerDept, String status);

    Mono<Void> softDelete(Long id);
}
