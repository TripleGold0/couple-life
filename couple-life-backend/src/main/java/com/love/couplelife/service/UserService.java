package com.love.couplelife.service;

import com.love.couplelife.dto.CompleteProfileDTO;
import com.love.couplelife.dto.UserProfileDTO;
import com.love.couplelife.vo.UserInfoVO;

/**
 * 用户业务接口。
 */
public interface UserService {

    /**
     * 获取当前登录用户的完整信息（含伴侣、邀请码、恋爱开始日期等）。
     */
    UserInfoVO currentUser();

    /**
     * 更新个人资料（昵称/性别/头像/生日，按非 null 字段更新）。
     */
    void updateProfile(UserProfileDTO dto);

    /**
     * 短信登录后强制完善资料（昵称、性别、密码），完成后置 profile_completed = 1。
     */
    void completeProfile(CompleteProfileDTO dto);

    /**
     * 个人信息页 —— 切换悬浮宠物显示开关（仅影响自身端展示，不影响数据累计）。
     *
     * @param enabled true 开启，false 关闭
     */
    void updatePetDisplay(boolean enabled);
}
