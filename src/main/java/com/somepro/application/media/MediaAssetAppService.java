package com.somepro.application.media;

import com.somepro.common.exception.BizException;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.repository.MediaAssetRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 素材用例编排（应用层）：登记 / 修改 / 查看 / 分页 / 删除。
 *
 * 不写业务规则（规则在领域层 MediaAsset），只做编排：
 * - 编号唯一性：先按编号查一次给出明确报错，数据库 uk_asset_code 唯一索引兜底
 *   （并发下两个相同编号同时登记时，后到的 insert 会撞索引，这里把它翻成业务异常）；
 * - 出入参都是领域对象，不认识 PO、也不认识 VO。
 */
@Service
public class MediaAssetAppService {

    private final MediaAssetRepository mediaAssetRepository;

    public MediaAssetAppService(MediaAssetRepository mediaAssetRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
    }

    /** 登记素材：编号不能撞，新素材默认 READY。 */
    public Mono<MediaAsset> register(String assetCode, String fileName, String mediaType,
                                     String fileExt, Long sizeBytes, Long durationMs,
                                     String checksum, String ownerDept) {
        MediaAsset asset = MediaAsset.register(assetCode, fileName, mediaType, fileExt,
                sizeBytes, durationMs, checksum, ownerDept);
        return mediaAssetRepository.findByAssetCode(asset.getAssetCode())
                .flatMap(exists -> Mono.<MediaAsset>error(
                        new BizException("素材编号已存在：" + asset.getAssetCode())))
                .switchIfEmpty(Mono.defer(() -> mediaAssetRepository.save(asset)))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BizException("素材编号已存在：" + asset.getAssetCode()));
    }

    /** 修改素材：先取出聚合，走领域行为改状态，再落库。编号变更时同样查重。 */
    public Mono<MediaAsset> update(Long id, String assetCode, String fileName, String mediaType,
                                   String fileExt, Long sizeBytes, Long durationMs,
                                   String checksum, String ownerDept, String status) {
        return mediaAssetRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("素材不存在：" + id)))
                .flatMap(asset -> {
                    String newCode = assetCode == null ? null : assetCode.trim();
                    if (newCode != null && !newCode.equals(asset.getAssetCode())) {
                        return mediaAssetRepository.findByAssetCode(newCode)
                                .flatMap(dup -> Mono.<MediaAsset>error(
                                        new BizException("素材编号已存在：" + newCode)))
                                .switchIfEmpty(Mono.defer(() -> {
                                    asset.revise(assetCode, fileName, mediaType, fileExt,
                                            sizeBytes, durationMs, checksum, ownerDept, status);
                                    return mediaAssetRepository.save(asset);
                                }));
                    }
                    asset.revise(assetCode, fileName, mediaType, fileExt,
                            sizeBytes, durationMs, checksum, ownerDept, status);
                    return mediaAssetRepository.save(asset);
                })
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BizException("素材编号已存在：" + assetCode));
    }

    public Mono<MediaAsset> get(Long id) {
        return mediaAssetRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("素材不存在：" + id)));
    }

    /** 分页翻查：条件都可空，一个都不填就是全量分页。 */
    public Mono<PageResult<MediaAsset>> page(int pageNum, int pageSize, String assetCode,
                                             String fileName, String mediaType,
                                             String ownerDept, String status) {
        return mediaAssetRepository.page(pageNum, pageSize, assetCode, fileName,
                normalize(mediaType), ownerDept, normalize(status));
    }

    public Mono<Void> delete(Long id) {
        return mediaAssetRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("素材不存在：" + id)))
                .flatMap(asset -> mediaAssetRepository.softDelete(id));
    }

    /** 枚举类查询条件统一转大写，调用方传小写也能查到。 */
    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toUpperCase();
    }
}
