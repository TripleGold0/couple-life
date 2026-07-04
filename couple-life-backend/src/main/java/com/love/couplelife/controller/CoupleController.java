package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.CoupleBindDTO;
import com.love.couplelife.service.CoupleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情侣关系控制器。
 * <p>
 * 业务模块：情侣绑定与解绑。绑定成功后双方进入同一个情侣空间，
 * 共享相册、打卡、旅行记录等数据；解绑会终止该共享关系。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/couple}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。
 * </p>
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {
    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    /**
     * 绑定伴侣，与指定用户建立情侣关系。
     * <p>HTTP: {@code POST /api/couple/bind}</p>
     *
     * @param dto 绑定参数（伴侣识别信息、恋爱开始日期等）
     * @return 无业务数据的成功结果
     */
    @PostMapping("/bind")
    public Result<Void> bind(@Valid @RequestBody CoupleBindDTO dto) {
        coupleService.bind(dto);
        return Result.success("绑定成功", null);
    }

    /**
     * 解除当前用户与其伴侣的绑定关系。
     * <p>HTTP: {@code POST /api/couple/unbind}</p>
     *
     * @return 无业务数据的成功结果
     */
    @PostMapping("/unbind")
    public Result<Void> unbind() {
        coupleService.unbind();
        return Result.success("已解除绑定", null);
    }
}
