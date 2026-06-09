package com.ycz.childnotesbackend.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.context.AdminAuthContext;
import com.ycz.childnotesbackend.filter.AuthTokenFilter;
import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.base.ResponseStateFactory;
import com.ycz.childnotesbackend.model.entity.AdminAccount;
import com.ycz.childnotesbackend.service.AdminAuthService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthService adminAuthService;

    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminAuthService adminAuthService, ObjectMapper objectMapper) {
        this.adminAuthService = adminAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        AdminAccount admin = adminAuthService.authenticate(resolveToken(request));
        if (admin == null) {
            writeUnauthorized(response);
            return false;
        }
        AdminAuthContext.setCurrentAdmin(admin);
        request.setAttribute("CURRENT_ADMIN", admin);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminAuthContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        Object token = request.getAttribute(AuthTokenFilter.TOKEN_ATTRIBUTE);
        if (token instanceof String && StringUtils.hasText((String) token)) {
            return (String) token;
        }
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (StringUtils.hasText(authorization)) {
            return authorization;
        }
        String headerToken = request.getHeader("token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        return request.getParameter("token");
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new Response<>(ResponseStateFactory.getFail().state(), "Admin login is required")
        ));
    }
}
