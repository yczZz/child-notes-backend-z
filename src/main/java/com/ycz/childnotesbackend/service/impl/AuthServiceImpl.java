package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.model.auth.AuthUser;
import com.ycz.childnotesbackend.model.dto.auth.LoginUserDto;
import com.ycz.childnotesbackend.model.dto.auth.UpdateProfileRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxCode2SessionResponse;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginResponse;
import com.ycz.childnotesbackend.model.dto.auth.WxUserInfoDto;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.service.AuthService;
import com.ycz.childnotesbackend.service.PointsService;
import com.ycz.childnotesbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String WX_CODE2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private static final String DEFAULT_WECHAT_NICK_NAME_PREFIX = "微信用户";
    private static final int DEFAULT_WECHAT_NICK_NAME_SUFFIX_LENGTH = 6;
    private static final char[] DEFAULT_WECHAT_NICK_NAME_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserMapper appUserMapper;

    private final PointsService pointsService;

    private final JwtUtil jwtUtil;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wechat.mini-app.app-id:}")
    private String appId;

    @Value("${wechat.mini-app.app-secret:}")
    private String appSecret;

    public AuthServiceImpl(AppUserMapper appUserMapper, PointsService pointsService, JwtUtil jwtUtil) {
        this.appUserMapper = appUserMapper;
        this.pointsService = pointsService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 微信小程序登录
     * WeChat mini-program login
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 调用微信 jscode2session 接口，使用前端临时 code 换取 openid 和 session_key
     *    Call WeChat jscode2session API to exchange the front-end code for openid and session_key
     * 2. 根据 openid 查询数据库用户，若不存在则判定为新用户并创建账号
     *    Query user by openid, create a new account if not found (newUser=true)
     * 3. 同步 unionid、session_key 等微信会话信息到用户表
     *    Sync unionid, session_key and other WeChat session info to user record
     * 4. 若用户为新用户或仍使用默认昵称，则更新微信个人资料（昵称/头像）
     *    Update WeChat profile (nickname/avatar) for new users or users with default profile
     * 5. 生成 JWT token 并写回数据库，完成登录态持久化
     *    Generate JWT token, persist it to DB for subsequent auth
     * 6. 若为新用户且请求中携带推荐码，则异步绑定推荐关系并发放邀请奖励
     *    Bind referrer relationship and grant invite reward if referrerId is provided for new user
     * 7. 封装并返回 token、过期时间、用户信息及是否新用户标志
     *    Return token, expiry, user info and newUser flag
     *
     * @param request 微信登录请求（code、userInfo、referrerId）/ WeChat login request
     * @return 登录响应（token + 用户信息 + 是否新用户）/ login response
     * @throws JsonProcessingException 解析微信接口返回的JSON时可能抛出 / thrown when parsing WeChat API response
     */
    @Override
    public WxLoginResponse wxLogin(WxLoginRequest request) throws JsonProcessingException {
        WxCode2SessionResponse session = code2Session(request.getCode());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tokenExpireAt = jwtUtil.defaultExpireAt();

        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getOpenid, session.getOpenid())
                .last("limit 1"));
        boolean newUser = user == null;
        if (user == null) {
            user = new AppUser();
            user.setOpenid(session.getOpenid());
            user.setCreatedAt(now);
        }

        user.setUnionid(session.getUnionid());
        user.setSessionKey(session.getSessionKey());
        applyUserInfo(user, request.getUserInfo(), newUser);
        user.setUpdatedAt(now);

        if (user.getId() == null) {
            appUserMapper.insert(user);
        } else {
            appUserMapper.updateById(user);
        }

        user.setToken(jwtUtil.createToken(user, tokenExpireAt));
        user.setTokenExpireAt(tokenExpireAt);
        user.setUpdatedAt(LocalDateTime.now());
        appUserMapper.updateById(user);
        if (newUser && StringUtils.hasText(request.getReferrerId())) {
            pointsService.bindReferrer(user.getId(), request.getReferrerId(), newUser);
        }

        WxLoginResponse response = new WxLoginResponse();
        response.setToken(user.getToken());
        response.setExpiresAt(tokenExpireAt.toString());
        response.setUserInfo(toLoginUser(user));
        response.setNewUser(newUser);
        return response;
    }

    /**
     * 根据请求头中的 Token 获取当前登录用户信息
     * Get current logged-in user information from the Authorization token
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 从 AuthContext 中取出已由过滤器解析好的用户身份
     *    Retrieve the user identity already parsed by the auth filter from AuthContext
     * 2. 根据用户ID查询数据库获取最新用户数据
     *    Query the database for the latest user data by user ID
     * 3. 转换为前端用户DTO并返回
     *    Convert to LoginUserDto and return
     *
     * @param authorization Authorization 请求头（已由过滤器验证）/ Authorization header (validated by filter)
     * @return 当前用户信息DTO / current user info DTO
     */
    @Override
    public LoginUserDto getUserByToken(String authorization) {
        return toLoginUser(findCurrentUser());
    }

    /**
     * 更新当前用户的个人资料
     * Update the current user's profile information
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 从 AuthContext 中获取并查询当前用户实体
     *    Retrieve the current user entity from AuthContext
     * 2. 逐字段判断：仅当请求字段非 null 时才覆盖，支持部分更新
     *    Partial update: only overwrite fields that are non-null in the request
     * 3. 更新 updatedAt 时间戳并持久化到数据库
     *    Update updatedAt timestamp and persist to database
     * 4. 返回更新后的用户信息
     *    Return updated user information
     *
     * @param authorization Authorization 请求头 / Authorization header
     * @param request       包含昵称、头像、性别等可更新字段 / updateable fields: nickname, avatarUrl, gender
     * @return 更新后的用户信息DTO / updated user info DTO
     */
    @Override
    public LoginUserDto updateProfile(String authorization, UpdateProfileRequest request) {
        AppUser user = findCurrentUser();
        if (request.getNickName() != null) {
            user.setNickName(request.getNickName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setUpdatedAt(LocalDateTime.now());
        appUserMapper.updateById(user);
        return toLoginUser(user);
    }

    /**
     * 从 AuthContext 中取出当前用户并查询完整的数据库记录
     * Retrieve the full user record for the currently authenticated user from AuthContext
     * <p>
     * 若未登录或用户不存在则抛出 401 异常
     * Throws 401 if not logged in or user does not exist in DB
     */
    private AppUser findCurrentUser() {
        AuthUser authUser = AuthContext.getCurrentUser();
        if (authUser == null || authUser.getId() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "未登录");
        }
        AppUser user = appUserMapper.selectById(authUser.getId());
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    /**
     * 调用微信 jscode2session 接口，用前端 code 换取 openid、unionid 和 session_key
     * Call WeChat jscode2session API to exchange the front-end code for openid, unionid and session_key
     * <p>
     * 若 appId / appSecret 未配置（本地开发环境），则返回 mock 数据（openid = "dev_" + code）
     * If appId/appSecret not configured (local dev), return mock data (openid = "dev_" + code)
     * 微信接口返回错误码或 openid 为空时，抛出 401 异常
     * Throws 401 if WeChat API returns error or openid is empty
     */
    private WxCode2SessionResponse code2Session(String code) throws JsonProcessingException {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            WxCode2SessionResponse response = new WxCode2SessionResponse();
            response.setOpenid("dev_" + code);
            response.setSessionKey("dev_session_key");
            return response;
        }

        String url = UriComponentsBuilder.fromHttpUrl(WX_CODE2_SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        String body = restTemplate.getForObject(url, String.class);
        WxCode2SessionResponse response = objectMapper.readValue(body, WxCode2SessionResponse.class);
        if (response == null || !StringUtils.hasText(response.getOpenid())) {
            String message = response == null ? "微信登录失败" : response.getErrmsg();
            throw new ResponseStatusException(UNAUTHORIZED, message);
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            throw new ResponseStatusException(UNAUTHORIZED, response.getErrmsg());
        }
        return response;
    }

    /**
     * 将微信登录时上报的用户信息同步到本地用户记录
     * Sync the WeChat user info (from login request) into the local user record
     * <p>
     * 同步策略 / Sync strategy:
     * - 仅对新用户或当前仍使用默认微信昵称的用户进行覆盖
     *   Only overwrite for new users or users still using the default WeChat nickname
     * - 各字段独立判断非空后才覆盖
     *   Each field is individually checked for non-empty before overwriting
     */
    private void applyUserInfo(AppUser user, WxUserInfoDto userInfo, boolean newUser) {
        boolean shouldRefreshProfile = newUser || isDefaultWechatProfile(user);
        if (!shouldRefreshProfile) {
            return;
        }
        applyNickName(user, userInfo == null ? null : userInfo.getNickName());
        if (userInfo == null) {
            return;
        }
        if (StringUtils.hasText(userInfo.getAvatarUrl())) {
            user.setAvatarUrl(userInfo.getAvatarUrl());
        }
        if (userInfo.getGender() != null) {
            user.setGender(userInfo.getGender());
        }
    }

    /**
     * 判断用户当前是否仍使用默认微信昵称（即未自定义过个人资料）
     * Check if the user is still using the default WeChat nickname (i.e., profile not customized)
     * <p>
     * 满足以下任一条件视为默认档案 / Considered default if any of the following:
     * - user 为 null / user is null
     * - 昵称为空 / nickname is empty
     * - 昵称等于 "微信用户" 或 "微信用户" + 6 位随机串 / nickname equals the WeChat default format
     */
    private boolean isDefaultWechatProfile(AppUser user) {
        return user == null
                || !StringUtils.hasText(user.getNickName())
                || isDefaultWechatNickName(user.getNickName());
    }

    private void applyNickName(AppUser user, String incomingNickName) {
        if (StringUtils.hasText(incomingNickName) && !isDefaultWechatNickName(incomingNickName)) {
            user.setNickName(incomingNickName);
            return;
        }
        if (!StringUtils.hasText(user.getNickName()) || DEFAULT_WECHAT_NICK_NAME_PREFIX.equals(user.getNickName())) {
            user.setNickName(generateDefaultWechatNickName());
        }
    }

    private boolean isDefaultWechatNickName(String nickName) {
        if (!StringUtils.hasText(nickName)) {
            return true;
        }
        if (DEFAULT_WECHAT_NICK_NAME_PREFIX.equals(nickName)) {
            return true;
        }
        int prefixLength = DEFAULT_WECHAT_NICK_NAME_PREFIX.length();
        if (!nickName.startsWith(DEFAULT_WECHAT_NICK_NAME_PREFIX)
                || nickName.length() != prefixLength + DEFAULT_WECHAT_NICK_NAME_SUFFIX_LENGTH) {
            return false;
        }
        for (int i = prefixLength; i < nickName.length(); i++) {
            char ch = nickName.charAt(i);
            if (!isDefaultNickNameSuffixChar(ch)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDefaultNickNameSuffixChar(char ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z');
    }

    private String generateDefaultWechatNickName() {
        StringBuilder nickName = new StringBuilder(DEFAULT_WECHAT_NICK_NAME_PREFIX);
        for (int i = 0; i < DEFAULT_WECHAT_NICK_NAME_SUFFIX_LENGTH; i++) {
            nickName.append(DEFAULT_WECHAT_NICK_NAME_CHARS[RANDOM.nextInt(DEFAULT_WECHAT_NICK_NAME_CHARS.length)]);
        }
        return nickName.toString();
    }

    /**
     * 将 AppUser 实体转换为前端可见的登录用户DTO
     * Convert AppUser entity to LoginUserDto visible to the front-end
     * <p>
     * 仅暴露必要字段：id、openid、昵称、头像、性别
     * Only exposes necessary fields: id, openid, nickname, avatarUrl, gender
     */
    private LoginUserDto toLoginUser(AppUser user) {
        LoginUserDto dto = new LoginUserDto();
        dto.setId(user.getId());
        dto.setOpenid(user.getOpenid());
        dto.setNickName(user.getNickName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setGender(user.getGender());
        return dto;
    }
}
