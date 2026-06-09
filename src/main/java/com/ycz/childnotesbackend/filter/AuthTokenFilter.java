package com.ycz.childnotesbackend.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_ATTRIBUTE = "AUTH_TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request.setAttribute(TOKEN_ATTRIBUTE, resolveToken(request));
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
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
}
