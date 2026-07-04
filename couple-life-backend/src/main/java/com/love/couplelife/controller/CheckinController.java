package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.CheckinDTO;
import com.love.couplelife.service.CheckinService;
import com.love.couplelife.vo.CheckinVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 每日打卡控制器。
 * <p>
 * 业务模块：情侣每日心情 / 状态打卡，提供打卡新增、按月日历视图查询、
 * 以及按指定日期查看自己与伴侣的打卡内容。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/checkins}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。
 * </p>
 */
@RestController
@RequestMapping("/api/checkins")
public class CheckinController {
    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /**
     * 新增一次打卡记录（每日通常仅允许打卡一次，由服务层校验）。
     * <p>HTTP: {@code POST /api/checkins}</p>
     *
     * @param dto 打卡内容（心情 emoji、心情文字、文字内容等）
     * @return 新生成打卡记录的 ID，键名为 {@code id}
     */
    @PostMapping
    public Result<Map<String, Long>> add(@Valid @RequestBody CheckinDTO dto) {
        return Result.success("打卡成功", checkinService.add(dto));
    }

    /**
     * 查询指定月份的打卡日历，用于前端日历视图展示当月每天的打卡情况。
     * <p>HTTP: {@code GET /api/checkins/calendar?month=yyyy-MM}</p>
     *
     * @param month 目标月份（格式 {@code yyyy-MM}）
     * @return 当月的打卡列表 {@link CheckinVO}
     */
    @GetMapping("/calendar")
    public Result<List<CheckinVO>> calendar(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(checkinService.calendar(month));
    }

    /**
     * 查询指定日期下情侣双方的打卡记录。
     * <p>HTTP: {@code GET /api/checkins/couple?date=yyyy-MM-dd}</p>
     *
     * @param date 目标日期（格式 {@code yyyy-MM-dd}）
     * @return 当日双方的打卡列表 {@link CheckinVO}
     */
    @GetMapping("/couple")
    public Result<List<CheckinVO>> couple(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(checkinService.coupleByDate(date));
    }
}
