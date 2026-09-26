package com.somepro.interfaces.rest.job;

import com.somepro.application.job.TranscodeJobAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.job.converter.TranscodeJobVoConverter;
import com.somepro.interfaces.rest.job.vo.TranscodeJobVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 转码任务接口（用户接口层）：提交 / 撤销 / 查看 / 分页。
 *
 * 只做协议适配（参数解析、VO 转换、返回包装），业务编排交给应用层：
 * - 统一返回 Mono<Result<T>>；
 * - 分页透传 pageNum/pageSize，不要写死；查询条件全部可空，一个都不填就是全量分页；
 * - ⚠️ 不直接返回领域对象：一律经 TranscodeJobVoConverter 转成 VO，
 *   否则 delFlag / createBy / updateBy 等内部字段会被序列化出去。
 */
@RestController
@RequestMapping("/api/transcode/job")
public class TranscodeJobController {

    private final TranscodeJobAppService transcodeJobAppService;

    public TranscodeJobController(TranscodeJobAppService transcodeJobAppService) {
        this.transcodeJobAppService = transcodeJobAppService;
    }

    /** 提交任务：一个素材按一个档位转一次；提出来是待处理 PENDING，任务编号自动发（TJ-2026-0001）。 */
    @PostMapping
    public Mono<Result<TranscodeJobVO>> submit(@RequestParam Long assetId,
                                               @RequestParam Long profileId,
                                               @RequestParam String ownerDept,
                                               @RequestParam(required = false) Integer priority) {
        return transcodeJobAppService.submit(assetId, profileId, ownerDept, priority)
                .map(TranscodeJobVoConverter::toVo)
                .map(Result::ok);
    }

    /** 撤销任务：只有待处理 PENDING 能撤，撤销原因必填。 */
    @PostMapping("/{id}/cancel")
    public Mono<Result<TranscodeJobVO>> cancel(@PathVariable Long id,
                                               @RequestParam String reason) {
        return transcodeJobAppService.cancel(id, reason)
                .map(TranscodeJobVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<TranscodeJobVO>> get(@PathVariable Long id) {
        return transcodeJobAppService.get(id)
                .map(TranscodeJobVoConverter::toVo)
                .map(Result::ok);
    }

    /** 分页翻查：编号/素材/档位/部门/状态精确；条件都可空。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<TranscodeJobVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(required = false) String jobNo,
                                                     @RequestParam(required = false) Long assetId,
                                                     @RequestParam(required = false) Long profileId,
                                                     @RequestParam(required = false) String ownerDept,
                                                     @RequestParam(required = false) String status) {
        return transcodeJobAppService.page(pageNum, pageSize, jobNo, assetId, profileId,
                        ownerDept, status)
                .map(TranscodeJobVoConverter::toPageVo)
                .map(Result::ok);
    }
}
