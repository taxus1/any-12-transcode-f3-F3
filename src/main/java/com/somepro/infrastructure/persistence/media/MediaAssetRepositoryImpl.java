package com.somepro.infrastructure.persistence.media;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.repository.MediaAssetRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.media.converter.MediaAssetPoConverter;
import com.somepro.infrastructure.persistence.media.po.MediaAssetPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 素材仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * 约定同 DemoItemRepositoryImpl：
 * - 所有 DB 调用必须经 {@link #blocking} 桥接到 boundedElastic，严禁在 event-loop 上跑 JDBC；
 * - Mapper 只认 {@link MediaAssetPO}，领域层只认 {@link MediaAsset}，两者在本类里互转；
 * - 软删除交给 @TableLogic，不手写 del_flag 条件；
 * - 分页统一用 PageHelper.startPage()，finally 里必须 clearPage()。
 */
@Repository
public class MediaAssetRepositoryImpl implements MediaAssetRepository {

    private final MediaAssetMapper mediaAssetMapper;

    public MediaAssetRepositoryImpl(MediaAssetMapper mediaAssetMapper) {
        this.mediaAssetMapper = mediaAssetMapper;
    }

    @Override
    public Mono<MediaAsset> save(MediaAsset asset) {
        return blocking(() -> {
            MediaAssetPO po = MediaAssetPoConverter.toPo(asset);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                mediaAssetMapper.insert(po);
            } else {
                mediaAssetMapper.updateById(po);
            }
            // insert 后框架会回填 id 与审计字段，转回领域对象一并返回
            return MediaAssetPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<MediaAsset> findById(Long id) {
        return blocking(() -> {
            MediaAssetPO po = mediaAssetMapper.selectById(id);
            // 返回 null 时 Mono.fromCallable 会自动转成空信号
            return po == null ? null : MediaAssetPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<MediaAsset> findByAssetCode(String assetCode) {
        return blocking(() -> {
            // uk_asset_code 唯一索引保证最多一条；@TableLogic 自动带 del_flag = 0
            MediaAssetPO po = mediaAssetMapper.selectOne(Wrappers.<MediaAssetPO>lambdaQuery()
                    .eq(MediaAssetPO::getAssetCode, assetCode));
            return po == null ? null : MediaAssetPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<MediaAsset>> page(int pageNum, int pageSize, String assetCode,
                                             String fileName, String mediaType,
                                             String ownerDept, String status) {
        return this.<PageResult<MediaAsset>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                // 条件全部可空：一个都不填时不拼任何条件，全量分页（早先录的数据也能翻出来）
                LambdaQueryWrapper<MediaAssetPO> wrapper = Wrappers.<MediaAssetPO>lambdaQuery()
                        .eq(StrUtil.isNotBlank(assetCode), MediaAssetPO::getAssetCode, assetCode)
                        .like(StrUtil.isNotBlank(fileName), MediaAssetPO::getFileName, fileName)
                        .eq(StrUtil.isNotBlank(mediaType), MediaAssetPO::getMediaType, mediaType)
                        .eq(StrUtil.isNotBlank(ownerDept), MediaAssetPO::getOwnerDept, ownerDept)
                        .eq(StrUtil.isNotBlank(status), MediaAssetPO::getStatus, status)
                        .orderByDesc(MediaAssetPO::getId);
                List<MediaAssetPO> rows = mediaAssetMapper.selectList(wrapper);
                // 命中分页插件时返回的是 com.github.pagehelper.Page，可直接取总数
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<MediaAsset> content = rows.stream()
                        .map(MediaAssetPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // 分页插件靠 ThreadLocal 传递分页参数，必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic 会把它翻译成 UPDATE t_media_asset SET del_flag = 1 WHERE id = ? AND del_flag = 0
            mediaAssetMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 阻塞 DB 调用 → 响应式链路的桥接器。
     *
     * 1. 先在响应式线程上从 Reactor Context 取操作人（切线程后就取不到了）
     * 2. 再切到 boundedElastic 执行 JDBC
     * 3. 把操作人放进 AuditContextHolder，供 MetaObjectHandler 填充 createBy / updateBy
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
