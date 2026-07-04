package com.love.couplelife.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.love.couplelife.entity.CoupleRelation;

/**
 * 情侣关系 Mapper。
 * <p>典型查询：按 userId 查找 status=1 的有效关系，进而获取 partnerId 与 coupleId。</p>
 */
public interface CoupleRelationMapper extends BaseMapper<CoupleRelation> {
}
