package com.love.couplelife.service;

import com.love.couplelife.dto.CheckinDTO;
import com.love.couplelife.vo.CheckinVO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 每日心情打卡业务接口。
 */
public interface CheckinService {

    /**
     * 新增/更新当日打卡（同一天 upsert）。
     *
     * @return 打卡记录 id
     */
    Map<String, Long> add(CheckinDTO dto);

    /**
     * 按月份返回当前用户的打卡日历。
     */
    List<CheckinVO> calendar(YearMonth month);

    /**
     * 返回指定日期"我和伴侣"的全部打卡（最多两条）。
     */
    List<CheckinVO> coupleByDate(LocalDate date);
}
