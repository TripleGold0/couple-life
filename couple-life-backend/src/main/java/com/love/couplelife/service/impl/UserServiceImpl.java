package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.CompleteProfileDTO;
import com.love.couplelife.dto.UserProfileDTO;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.UserService;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.UserInfoVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户基础服务实现：当前用户信息查询、资料修改、短信登录后的资料补全。
 *
 * <p>协作组件：
 * <ul>
 *     <li>{@link UserMapper}：用户表 CRUD</li>
 *     <li>{@link CoupleRelationMapper}：用于在 currentUser() 中带出伴侣信息和恋爱起始日期</li>
 *     <li>{@link PasswordEncoder}：completeProfile 阶段对新设密码加密</li>
 *     <li>{@link SecurityUtil}：从 JWT 安全上下文获取当前用户 id</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>profileCompleted=0 表示短信登录自动建号、尚未补完资料的过渡状态</li>
 *     <li>currentUser() 同时返回伴侣 VO，便于前端一次渲染情侣页面头部</li>
 *     <li>updateProfile 仅对显式传入且非空的字段做局部更新</li>
 * </ul>
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final CoupleRelationMapper coupleRelationMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, CoupleRelationMapper coupleRelationMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.coupleRelationMapper = coupleRelationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 将 {@link User} 实体转换为对外的 {@link UserInfoVO}。
     * <p>不返回密码、邀请码等敏感/特殊字段（邀请码由 {@link #currentUser} 单独补充）。
     * profileCompleted 在历史数据为 null 时按已完成处理（兼容老用户）。
     */
    public UserInfoVO toVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setProfileCompleted(user.getProfileCompleted() == null ? 1 : user.getProfileCompleted());
        // 历史用户在新增字段前 pet_display_enabled 可能为 null，按"默认开启"处理
        vo.setPetDisplayEnabled(user.getPetDisplayEnabled() == null ? 1 : user.getPetDisplayEnabled());
        return vo;
    }

    /**
     * 获取当前登录用户信息，附带邀请码、恋爱起始日期及伴侣信息（如已绑定）。
     *
     * @throws BizException 用户不存在
     */
    @Override
    public UserInfoVO currentUser() {
        Long userId = SecurityUtil.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserInfoVO vo = toVO(user);
        vo.setInviteCode(user.getInviteCode());
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation != null) {
            vo.setLoveStartDate(relation.getLoveStartDate());
            User partner = userMapper.selectById(relation.getPartnerId());
            if (partner != null) {
                vo.setPartner(toVO(partner));
            }
        }
        return vo;
    }

    /**
     * 局部更新当前用户资料（昵称、性别、头像、生日）。
     *
     * <p>仅对入参中显式提供且非空的字段进行覆盖，其余字段保持不变。
     *
     * @throws BizException 用户不存在
     */
    @Override
    public void updateProfile(UserProfileDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBirthday() != null) {
            user.setBirthday(dto.getBirthday());
        }
        userMapper.updateById(user);
    }

    /**
     * 资料补全：用于短信登录自动建号后第一次完善昵称、性别、密码。
     *
     * <p>完成后将 profileCompleted 置为 1，标志账号进入完整可用状态。
     *
     * @throws BizException 用户不存在
     */
    @Override
    public void completeProfile(CompleteProfileDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setNickname(dto.getNickname().trim());
        user.setGender(dto.getGender());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setProfileCompleted(1);
        userMapper.updateById(user);
    }

    /**
     * 切换"悬浮宠物是否在我侧显示"的偏好开关。
     *
     * <p>注意：该开关仅影响当前用户的网页端是否渲染悬浮挂件，不影响：
     * <ul>
     *     <li>另一半的展示</li>
     *     <li>宠物属性（亲密度、饱食度等）的衰减与累计</li>
     *     <li>双方互动记录的数据写入</li>
     * </ul>
     */
    @Override
    public void updatePetDisplay(boolean enabled) {
        Long userId = SecurityUtil.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPetDisplayEnabled(enabled ? 1 : 0);
        userMapper.updateById(user);
    }
}
