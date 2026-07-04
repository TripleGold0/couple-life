package com.love.couplelife.service;

import com.love.couplelife.dto.CoupleBindDTO;

/**
 * 情侣关系业务接口。
 */
public interface CoupleService {

    /**
     * 通过对方邀请码建立双向情侣关系。
     */
    void bind(CoupleBindDTO dto);

    /**
     * 解除当前用户与伴侣的情侣关系（双向 status 置为 0）。
     */
    void unbind();
}
