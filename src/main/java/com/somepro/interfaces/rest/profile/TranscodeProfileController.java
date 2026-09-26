package com.somepro.interfaces.rest.profile;

import com.somepro.application.profile.TranscodeProfileAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.profile.converter.TranscodeProfileVoConverter;
import com.somepro.interfaces.rest.profile.vo.TranscodeProfileVO;
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
 * 转码档位底账接口（用户接口层）：登记 / 修改 / 查看 / 分页 / 删除。
 *
 * 只做协议适配（参数解析、VO 转换、返回包装），业务编排交给应用层：
 * - 统一返回 Mono<Result<T>>；
 * - 分页透传 pageNum/pageSize，不要写死；查询条件全部可空，一个都不填就是全量分页；
 * - ⚠️ 不直接返回领域对象：一律经 TranscodeProfileVoConverter 转成 VO，
 *   否则 delFlag / createBy / updateBy 等内部字段会被序列化出去。
 */
@RestController
@RequestMapping("/api/transcode/profile")
public class TranscodeProfileController {

    private final TranscodeProfileAppService transcodeProfileAppService;

    public TranscodeProfileController(TranscodeProfileAppService transcodeProfileAppService) {
        this.transcodeProfileAppService = transcodeProfileAppService;
    }

    /** 登记档位：编号唯一，新档位默认 ENABLED；音频档位的宽/高/视频码率会被置空。 */
    @PostMapping
    public Mono<Result<TranscodeProfileVO>> register(@RequestParam String profileCode,
                                                     @RequestParam String profileName,
                                                     @RequestParam String mediaType,
                                                     @RequestParam String targetFormat,
                                                     @RequestParam(required = false) Integer width,
                                                     @RequestParam(required = false) Integer height,
                                                     @RequestParam(required = false) Integer videoBitrateKbps,
                                                     @RequestParam(required = false) Integer audioBitrateKbps) {
        return transcodeProfileAppService.register(profileCode, profileName, mediaType, targetFormat,
                        width, height, videoBitrateKbps, audioBitrateKbps)
                .map(TranscodeProfileVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改档位：status 不传表示不动当前状态。 */
    @PutMapping("/{id}")
    public Mono<Result<TranscodeProfileVO>> update(@PathVariable Long id,
                                                   @RequestParam String profileCode,
                                                   @RequestParam String profileName,
                                                   @RequestParam String mediaType,
                                                   @RequestParam String targetFormat,
                                                   @RequestParam(required = false) Integer width,
                                                   @RequestParam(required = false) Integer height,
                                                   @RequestParam(required = false) Integer videoBitrateKbps,
                                                   @RequestParam(required = false) Integer audioBitrateKbps,
                                                   @RequestParam(required = false) String status) {
        return transcodeProfileAppService.update(id, profileCode, profileName, mediaType, targetFormat,
                        width, height, videoBitrateKbps, audioBitrateKbps, status)
                .map(TranscodeProfileVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<TranscodeProfileVO>> get(@PathVariable Long id) {
        return transcodeProfileAppService.get(id)
                .map(TranscodeProfileVoConverter::toVo)
                .map(Result::ok);
    }

    /** 分页翻查：编号精确、名字模糊、类型/状态精确；条件都可空。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<TranscodeProfileVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String profileCode,
                                                         @RequestParam(required = false) String profileName,
                                                         @RequestParam(required = false) String mediaType,
                                                         @RequestParam(required = false) String status) {
        return transcodeProfileAppService.page(pageNum, pageSize, profileCode, profileName,
                        mediaType, status)
                .map(TranscodeProfileVoConverter::toPageVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return transcodeProfileAppService.delete(id)
                .thenReturn(Result.ok());
    }
}
