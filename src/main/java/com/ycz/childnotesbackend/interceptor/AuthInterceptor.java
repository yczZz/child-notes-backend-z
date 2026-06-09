package com.ycz.childnotesbackend.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.filter.AuthTokenFilter;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.model.auth.AuthUser;
import com.ycz.childnotesbackend.model.auth.JwtPayload;
import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.base.ResponseStateFactory;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final AppUserMapper appUserMapper;

    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, AppUserMapper appUserMapper, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.appUserMapper = appUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = (String) request.getAttribute(AuthTokenFilter.TOKEN_ATTRIBUTE);
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "未登录");
            return false;
        }

        try {
            JwtPayload payload = jwtUtil.parseToken(token);
            AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getId, payload.getUserId())
                    .eq(AppUser::getOpenid, payload.getOpenid())
                    .last("limit 1"));
            if (user == null) {
                writeUnauthorized(response, "用户不存在");
                return false;
            }
            AuthContext.setCurrentUser(toAuthUser(user));
            request.setAttribute("CURRENT_USER", AuthContext.getCurrentUser());
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private AuthUser toAuthUser(AppUser user) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setOpenid(user.getOpenid());
        authUser.setNickName(user.getNickName());
        authUser.setAvatarUrl(user.getAvatarUrl());
        authUser.setGender(user.getGender());
        return authUser;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new Response<>(ResponseStateFactory.getFail().state(), message)
        ));
    }
}
