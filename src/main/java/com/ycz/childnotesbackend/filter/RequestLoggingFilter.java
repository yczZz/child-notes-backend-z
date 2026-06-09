package com.ycz.childnotesbackend.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
        filterChain.doFilter(wrapper, response);

        String method = wrapper.getMethod();
        String uri = wrapper.getRequestURI();
        String query = wrapper.getQueryString();
        byte[] body = wrapper.getContentAsByteArray();

        String url = query != null ? uri + "?" + query : uri;
        String bodyStr = body.length > 0 ? new String(body, StandardCharsets.UTF_8) : "";

        log.info("[api] {} {} {}", method, url, bodyStr);
    }
}
