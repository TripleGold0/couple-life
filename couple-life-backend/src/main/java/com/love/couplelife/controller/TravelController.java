package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.TravelRecordDTO;
import com.love.couplelife.dto.TravelUpdateDTO;
import com.love.couplelife.service.TravelService;
import com.love.couplelife.vo.TravelRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 旅行记录控制器。
 * <p>
 * 业务模块：情侣旅行回忆管理，提供旅行记录的增删改查（CRUD），
 * 数据维度归属当前用户所在的情侣空间。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/travels}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权），通常要求已绑定情侣。
 * </p>
 */
@RestController
@RequestMapping("/api/travels")
public class TravelController {
    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    /**
     * 新增一条旅行记录。
     * <p>HTTP: {@code POST /api/travels}</p>
     *
     * @param dto 旅行记录数据（地点、时间区间、描述、图片等）
     * @return 新生成记录的 ID，键名为 {@code id}
     */
    @PostMapping
    public Result<Map<String, Long>> add(@Valid @RequestBody TravelRecordDTO dto) {
        return Result.success("新增成功", travelService.add(dto));
    }

    /**
     * 查询当前情侣空间下的全部旅行记录列表。
     * <p>HTTP: {@code GET /api/travels}</p>
     *
     * @return 旅行记录列表 {@link TravelRecordVO}
     */
    @GetMapping
    public Result<List<TravelRecordVO>> list() {
        return Result.success(travelService.list());
    }

    /**
     * 查询指定旅行记录详情。
     * <p>HTTP: {@code GET /api/travels/{id}}</p>
     *
     * @param id 旅行记录 ID
     * @return 旅行记录详情 {@link TravelRecordVO}
     */
    @GetMapping("/{id}")
    public Result<TravelRecordVO> detail(@PathVariable Long id) {
        return Result.success(travelService.detail(id));
    }

    /**
     * 修改指定旅行记录。
     * <p>HTTP: {@code PUT /api/travels/{id}}</p>
     *
     * @param id  旅行记录 ID
     * @param dto 修改后的字段集合（仅更新非 null 字段）
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TravelUpdateDTO dto) {
        travelService.update(id, dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除指定旅行记录（通常为逻辑删除）。
     * <p>HTTP: {@code DELETE /api/travels/{id}}</p>
     *
     * @param id 旅行记录 ID
     * @return 无业务数据的成功结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        travelService.delete(id);
        return Result.success("删除成功", null);
    }
}
