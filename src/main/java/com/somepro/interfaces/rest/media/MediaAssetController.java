package com.somepro.interfaces.rest.media;

import com.somepro.application.media.MediaAssetAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.media.converter.MediaAssetVoConverter;
import com.somepro.interfaces.rest.media.vo.MediaAssetVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 素材底账接口（用户接口层）：登记 / 修改 / 查看 / 分页 / 删除。
 *
 * 只做协议适配（参数解析、VO 转换、返回包装），业务编排交给应用层：
 * - 统一返回 Mono<Result<T>>；
 * - 分页透传 pageNum/pageSize，不要写死；查询条件全部可空，一个都不填就是全量分页；
 * - ⚠️ 不直接返回领域对象：一律经 MediaAssetVoConverter 转成 VO，
 *   否则 delFlag / createBy / updateBy 等内部字段会被序列化出去。
 */
@RestController
@RequestMapping("/api/media/asset")
public class MediaAssetController {

    private final MediaAssetAppService mediaAssetAppService;

    public MediaAssetController(MediaAssetAppService mediaAssetAppService) {
        this.mediaAssetAppService = mediaAssetAppService;
    }

    /** 登记素材：编号唯一，新素材默认 READY。 */
    @PostMapping
    public Mono<Result<MediaAssetVO>> register(@RequestParam String assetCode,
                                               @RequestParam String fileName,
                                               @RequestParam String mediaType,
                                               @RequestParam(required = false) String fileExt,
                                               @RequestParam(required = false) Long sizeBytes,
                                               @RequestParam(required = false) Long durationMs,
                                               @RequestParam(required = false) String checksum,
                                               @RequestParam String ownerDept) {
        return mediaAssetAppService.register(assetCode, fileName, mediaType, fileExt,
                        sizeBytes, durationMs, checksum, ownerDept)
                .map(MediaAssetVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改素材：status 不传表示不动当前状态。 */
    @PutMapping("/{id}")
    public Mono<Result<MediaAssetVO>> update(@PathVariable Long id,
                                             @RequestParam String assetCode,
                                             @RequestParam String fileName,
                                             @RequestParam String mediaType,
                                             @RequestParam(required = false) String fileExt,
                                             @RequestParam(required = false) Long sizeBytes,
                                             @RequestParam(required = false) Long durationMs,
                                             @RequestParam(required = false) String checksum,
                                             @RequestParam String ownerDept,
                                             @RequestParam(required = false) String status) {
        return mediaAssetAppService.update(id, assetCode, fileName, mediaType, fileExt,
                        sizeBytes, durationMs, checksum, ownerDept, status)
                .map(MediaAssetVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<MediaAssetVO>> get(@PathVariable Long id) {
        return mediaAssetAppService.get(id)
                .map(MediaAssetVoConverter::toVo)
                .map(Result::ok);
    }

    /** 分页翻查：编号精确、文件名模糊、类型/部门/状态精确；条件都可空。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<MediaAssetVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam(required = false) String assetCode,
                                                   @RequestParam(required = false) String fileName,
                                                   @RequestParam(required = false) String mediaType,
                                                   @RequestParam(required = false) String ownerDept,
                                                   @RequestParam(required = false) String status) {
        return mediaAssetAppService.page(pageNum, pageSize, assetCode, fileName,
                        mediaType, ownerDept, status)
                .map(MediaAssetVoConverter::toPageVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return mediaAssetAppService.delete(id)
                .thenReturn(Result.ok());
    }
}
