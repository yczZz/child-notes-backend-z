package com.ycz.childnotesbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ycz.childnotesbackend.model.dto.auth.LoginUserDto;
import com.ycz.childnotesbackend.model.dto.auth.UpdateProfileRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginResponse;

public interface AuthService {

    /**
     * 微信小程序登录
     * WeChat mini-program login
     *
     * @param request 微信登录请求参数 / WeChat login request parameters
     * @return 微信登录响应 / WeChat login response
     * @throws JsonProcessingException JSON处理异常 / JSON processing exception
     */
    WxLoginResponse wxLogin(WxLoginRequest request) throws JsonProcessingException;

    /**
     * 根据Token获取用户信息
     * Get user information by token
     *
     * @param authorization 授权令牌 / authorization token
     * @return 用户信息 / user information
     */
    LoginUserDto getUserByToken(String authorization);

    /**
     * 更新用户个人资料
     * Update user profile
     *
     * @param authorization 授权令牌 / authorization token
     * @param request 更新资料请求参数 / update profile request parameters
     * @return 更新后的用户信息 / updated user information
     */
    LoginUserDto updateProfile(String authorization, UpdateProfileRequest request);
}
