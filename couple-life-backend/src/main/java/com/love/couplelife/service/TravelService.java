package com.love.couplelife.service;

import com.love.couplelife.dto.TravelRecordDTO;
import com.love.couplelife.dto.TravelUpdateDTO;
import com.love.couplelife.vo.TravelRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 旅行记录业务接口。
 */
public interface TravelService {

    /**
     * 新增旅行记录（同时落库图片关联）。
     *
     * @return 新记录 id
     */
    Map<String, Long> add(TravelRecordDTO dto);

    /**
     * 列表查询，按 travelDate 倒序。
     */
    List<TravelRecordVO> list();

    /**
     * 详情查询，包含全部关联图片。
     */
    TravelRecordVO detail(Long id);

    /**
     * 更新指定旅行记录。仅创建者所在的情侣空间可改。
     */
    void update(Long id, TravelRecordDTO dto);

    /**
     * 部分更新指定旅行记录（仅更新非 null 字段）。
     */
    void update(Long id, TravelUpdateDTO dto);

    /**
     * 软删除指定旅行记录。
     */
    void delete(Long id);
}
