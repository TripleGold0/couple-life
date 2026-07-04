package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.TravelRecordDTO;
import com.love.couplelife.dto.TravelUpdateDTO;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.TravelImage;
import com.love.couplelife.entity.TravelRecord;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.TravelImageMapper;
import com.love.couplelife.mapper.TravelRecordMapper;
import com.love.couplelife.service.TravelService;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.TravelRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 情侣旅行记录服务实现。
 *
 * <p>职责：维护一对情侣共同的旅行记录（地点、行程详情、双方感受）及其图片。
 * <p>协作组件：
 * <ul>
 *     <li>{@link TravelRecordMapper}：旅行主记录 CRUD（软删除：deleted=1）</li>
 *     <li>{@link TravelImageMapper}：旅行图片表，按 sort_order 排序</li>
 *     <li>{@link CoupleRelationMapper}：校验情侣关系并取出 coupleId</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>所有操作必须先绑定情侣（status=1），否则抛 {@link BizException}</li>
 *     <li>按 coupleId 做数据隔离，跨情侣无法访问/修改</li>
 *     <li>更新时仅当 imageUrls 显式非 null 才覆盖图片，避免误清空</li>
 *     <li>删除为软删除（deleted=1），列表/详情自动过滤</li>
 * </ul>
 */
@Service
public class TravelServiceImpl implements TravelService {
    private final TravelRecordMapper travelRecordMapper;
    private final TravelImageMapper travelImageMapper;
    private final CoupleRelationMapper coupleRelationMapper;

    public TravelServiceImpl(TravelRecordMapper travelRecordMapper, TravelImageMapper travelImageMapper, CoupleRelationMapper coupleRelationMapper) {
        this.travelRecordMapper = travelRecordMapper;
        this.travelImageMapper = travelImageMapper;
        this.coupleRelationMapper = coupleRelationMapper;
    }

    /**
     * 新增一条旅行记录及其图片。
     *
     * @param dto 旅行记录信息（含图片 URL 列表）
     * @return 新记录主键 id
     * @throws BizException 当前用户未绑定情侣关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> add(TravelRecordDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        Long coupleId = requireCoupleId(userId);
        TravelRecord record = fill(new TravelRecord(), dto);
        record.setCoupleId(coupleId);
        record.setCreatorId(userId);
        record.setDeleted(0);
        travelRecordMapper.insert(record);
        saveImages(record.getId(), dto.getImageUrls());
        return Map.of("id", record.getId());
    }

    /**
     * 列出当前情侣全部旅行记录（不含 detail / 双方感受），按 travelDate 倒序。
     *
     * @return 旅行记录摘要 VO 列表
     */
    @Override
    public List<TravelRecordVO> list() {
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        return travelRecordMapper.selectList(new LambdaQueryWrapper<TravelRecord>()
                        .eq(TravelRecord::getCoupleId, coupleId)
                        .eq(TravelRecord::getDeleted, 0)
                        .orderByDesc(TravelRecord::getTravelDate))
                .stream().map(record -> toVO(record, false)).toList();
    }

    /**
     * 查询单条旅行记录的完整详情（含 detail 与双方感受）。
     *
     * @param id 旅行记录 id
     * @throws BizException 记录不存在或不属于当前情侣
     */
    @Override
    public TravelRecordVO detail(Long id) {
        TravelRecord record = requireRecord(id);
        return toVO(record, true);
    }

    /**
     * 修改旅行记录。
     *
     * <p>仅当 dto.imageUrls 显式非 null 时才覆盖图片：先全量删除原图，再按顺序重插。
     * 这样允许调用方"只改文本字段"而不影响图片列表。
     *
     * @throws BizException 记录不存在或不属于当前情侣
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TravelRecordDTO dto) {
        TravelRecord record = requireRecord(id);
        fill(record, dto);
        travelRecordMapper.updateById(record);
        // 仅在显式传入 imageUrls 时才覆盖图片，避免误传 null 时把已有图片全部清空
        if (dto.getImageUrls() != null) {
            travelImageMapper.delete(new LambdaQueryWrapper<TravelImage>().eq(TravelImage::getTravelId, id));
            saveImages(id, dto.getImageUrls());
        }
    }

    /**
     * 部分修改旅行记录（仅更新非 null 字段）。
     *
     * <p>适用于只需要更新部分字段（如只更新 myFeeling）的场景。
     *
     * @throws BizException 记录不存在或不属于当前情侣
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TravelUpdateDTO dto) {
        TravelRecord record = requireRecord(id);
        fillPartial(record, dto);
        travelRecordMapper.updateById(record);
        // 仅在显式传入 imageUrls 时才覆盖图片
        if (dto.getImageUrls() != null) {
            travelImageMapper.delete(new LambdaQueryWrapper<TravelImage>().eq(TravelImage::getTravelId, id));
            saveImages(id, dto.getImageUrls());
        }
    }

    /**
     * 软删除旅行记录（deleted=1），保留图片记录不动。
     *
     * @throws BizException 记录不存在或不属于当前情侣
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        travelRecordMapper.update(null, new LambdaUpdateWrapper<TravelRecord>()
                .eq(TravelRecord::getId, id)
                .eq(TravelRecord::getCoupleId, coupleId)
                .eq(TravelRecord::getDeleted, 0)
                .set(TravelRecord::getDeleted, 1));
    }

    /** 校验旅行记录存在、未删除并且属于当前情侣，否则抛业务异常。 */
    private TravelRecord requireRecord(Long id) {
        Long coupleId = requireCoupleId(SecurityUtil.currentUserId());
        TravelRecord record = travelRecordMapper.selectById(id);
        if (record == null || record.getDeleted() == 1 || !record.getCoupleId().equals(coupleId)) {
            throw new BizException("旅游记录不存在");
        }
        return record;
    }

    /** 取当前用户已绑定的 coupleId；未绑定抛业务异常。 */
    private Long requireCoupleId(Long userId) {
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation == null) {
            throw new BizException("请先绑定情侣关系");
        }
        return relation.getCoupleId();
    }

    /** 把 DTO 的可变字段拷贝到实体，不涉及 coupleId / creatorId / deleted 等系统字段。 */
    private TravelRecord fill(TravelRecord record, TravelRecordDTO dto) {
        record.setLocationName(dto.getLocationName());
        record.setCountry(dto.getCountry());
        record.setCity(dto.getCity());
        record.setLongitude(dto.getLongitude());
        record.setLatitude(dto.getLatitude());
        record.setTravelDate(dto.getTravelDate());
        record.setSummary(dto.getSummary());
        record.setDetail(dto.getDetail());
        record.setMyFeeling(dto.getMyFeeling());
        record.setPartnerFeeling(dto.getPartnerFeeling());
        return record;
    }

    /** 把 DTO 的非 null 字段拷贝到实体（部分更新）。 */
    private TravelRecord fillPartial(TravelRecord record, TravelUpdateDTO dto) {
        if (dto.getLocationName() != null) record.setLocationName(dto.getLocationName());
        if (dto.getCountry() != null) record.setCountry(dto.getCountry());
        if (dto.getCity() != null) record.setCity(dto.getCity());
        if (dto.getLongitude() != null) record.setLongitude(dto.getLongitude());
        if (dto.getLatitude() != null) record.setLatitude(dto.getLatitude());
        if (dto.getTravelDate() != null) record.setTravelDate(dto.getTravelDate());
        if (dto.getSummary() != null) record.setSummary(dto.getSummary());
        if (dto.getDetail() != null) record.setDetail(dto.getDetail());
        if (dto.getMyFeeling() != null) record.setMyFeeling(dto.getMyFeeling());
        if (dto.getPartnerFeeling() != null) record.setPartnerFeeling(dto.getPartnerFeeling());
        return record;
    }

    /** 按入参顺序保存图片，sortOrder 即下标，前端展示按此顺序。 */
    private void saveImages(Long travelId, List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            TravelImage image = new TravelImage();
            image.setTravelId(travelId);
            image.setImageUrl(imageUrls.get(i));
            image.setSortOrder(i);
            travelImageMapper.insert(image);
        }
    }

    /**
     * 实体 → VO；列表场景 withDetail=false（不返回大字段），详情场景 withDetail=true。
     * 封面图取按 sortOrder 升序的第一张图片。
     */
    private TravelRecordVO toVO(TravelRecord record, boolean withDetail) {
        List<String> images = travelImageMapper.selectList(new LambdaQueryWrapper<TravelImage>()
                        .eq(TravelImage::getTravelId, record.getId())
                        .orderByAsc(TravelImage::getSortOrder))
                .stream().map(TravelImage::getImageUrl).toList();
        TravelRecordVO vo = new TravelRecordVO();
        vo.setId(record.getId());
        vo.setLocationName(record.getLocationName());
        vo.setCountry(record.getCountry());
        vo.setCity(record.getCity());
        vo.setLongitude(record.getLongitude());
        vo.setLatitude(record.getLatitude());
        vo.setTravelDate(record.getTravelDate());
        vo.setSummary(record.getSummary());
        vo.setCoverImage(images.isEmpty() ? null : images.get(0));
        vo.setImages(images);
        if (withDetail) {
            vo.setDetail(record.getDetail());
            vo.setMyFeeling(record.getMyFeeling());
            vo.setPartnerFeeling(record.getPartnerFeeling());
        }
        return vo;
    }
}
