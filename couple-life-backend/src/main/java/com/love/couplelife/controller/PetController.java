package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.PetInteractionDTO;
import com.love.couplelife.dto.PetSelectionRequestDTO;
import com.love.couplelife.service.PetService;
import com.love.couplelife.vo.PetSelectionRequestVO;
import com.love.couplelife.vo.PetTypeVO;
import com.love.couplelife.vo.PetVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电子宠物控制器。
 *
 * <p>统一前缀：{@code /api/pet}，全部接口需要 JWT 鉴权。</p>
 *
 * <p>接口一览：
 * <ul>
 *     <li>{@code GET    /api/pet/current}                            当前情侣宠物详情</li>
 *     <li>{@code GET    /api/pet/types}                              可选宠物列表</li>
 *     <li>{@code GET    /api/pet/selection/list}                     与我相关的选择请求</li>
 *     <li>{@code POST   /api/pet/selection/request}                  发起选择 / 更换请求</li>
 *     <li>{@code POST   /api/pet/selection/{id}/agree}               同意请求</li>
 *     <li>{@code POST   /api/pet/selection/{id}/reject}              拒绝请求</li>
 *     <li>{@code POST   /api/pet/interact}                           互动（喂食 / 抚摸 / 玩耍）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pet")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /** 当前情侣的宠物详情；未绑定情侣或未选择宠物时 data 为 null。 */
    @GetMapping("/current")
    public Result<PetVO> current() {
        return Result.success(petService.currentPet());
    }

    /** 平台上架的可选宠物种类列表。 */
    @GetMapping("/types")
    public Result<List<PetTypeVO>> types() {
        return Result.success(petService.listTypes());
    }

    /** 与我相关（自己发起 / 需我处理）的选择请求列表，按时间倒序。 */
    @GetMapping("/selection/list")
    public Result<List<PetSelectionRequestVO>> selectionList() {
        return Result.success(petService.listSelectionRequests());
    }

    /** 发起一次宠物选择 / 更换请求，等待对方同意。 */
    @PostMapping("/selection/request")
    public Result<PetSelectionRequestVO> createSelection(@Valid @RequestBody PetSelectionRequestDTO dto) {
        return Result.success("已发送给对方，等待同意", petService.createSelectionRequest(dto));
    }

    /** 同意一个待处理请求，成功时返回新创建的宠物。 */
    @PostMapping("/selection/{id}/agree")
    public Result<PetVO> agree(@PathVariable("id") Long id) {
        return Result.success("已同意，宠物已生效", petService.agreeSelectionRequest(id));
    }

    /** 拒绝一个待处理请求。 */
    @PostMapping("/selection/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id) {
        petService.rejectSelectionRequest(id);
        return Result.success("已拒绝", null);
    }

    /** 与宠物互动一次（喂食 / 抚摸 / 玩耍），返回最新状态。 */
    @PostMapping("/interact")
    public Result<PetVO> interact(@Valid @RequestBody PetInteractionDTO dto) {
        return Result.success(petService.interact(dto));
    }
}
