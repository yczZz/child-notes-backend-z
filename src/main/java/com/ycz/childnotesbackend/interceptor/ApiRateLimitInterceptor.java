package com.ycz.childnotesbackend.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.config.RateLimitProperties;
import com.ycz.childnotesbackend.mapper.IpBlacklistMapper;
import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.base.ResponseStateFactory;
import com.ycz.childnotesbackend.model.entity.IpBlacklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitInterceptor.class);

    private static final long WINDOW_MILLIS = 1000L;

    private static final long CLEANUP_INTERVAL_MILLIS = 60000L;

    private static final long COUNTER_TTL_MILLIS = 120000L;

    private final RateLimitProperties properties;

    private final IpBlacklistMapper ipBlacklistMapper;

    private final ObjectMapper objectMapper;

    private final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();

    private final ConcurrentMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    private final AtomicLong lastCleanupAt = new AtomicLong(0L);

    public ApiRateLimitInterceptor(RateLimitProperties properties, IpBlacklistMapper ipBlacklistMapper, ObjectMapper objectMapper) {
        this.properties = properties;
        this.ipBlacklistMapper = ipBlacklistMapper;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadBlacklistedIps() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            List<IpBlacklist> records = ipBlacklistMapper.selectList(new LambdaQueryWrapper<IpBlacklist>()
                    .select(IpBlacklist::getIpAddress));
            records.stream()
                    .map(IpBlacklist::getIpAddress)
                    .filter(StringUtils::hasText)
                    .forEach(blacklistedIps::add);
            log.info("Loaded {} blacklisted IPs", blacklistedIps.size());
        } catch (Exception e) {
            log.warn("Failed to load IP blacklist", e);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String ip = resolveClientIp(request);
        if (blacklistedIps.contains(ip)) {
            writeLimited(response, HttpServletResponse.SC_FORBIDDEN, "当前IP已被限制访问");
            return false;
        }

        String endpoint = resolveEndpoint(request);
        String key = ip + "|" + endpoint;
        long now = System.currentTimeMillis();
        int count = addRequestAndGetCount(key, now);
        cleanupCounters(now);

        int blacklistThreshold = Math.max(properties.getBlacklistRequestsPerSecond(), properties.getMaxRequestsPerSecond() + 1);
        if (count > blacklistThreshold) {
            blacklistIp(ip, request.getMethod(), request.getRequestURI(), endpoint, count, now);
            writeLimited(response, HttpServletResponse.SC_FORBIDDEN, "请求过于频繁，当前IP已被永久限制访问");
            return false;
        }

        if (count > properties.getMaxRequestsPerSecond()) {
            response.setHeader("Retry-After", "1");
            writeLimited(response, 429, "请求过于频繁，请稍后再试");
            return false;
        }

        return true;
    }

    private int addRequestAndGetCount(String key, long now) {
        RequestCounter counter = counters.computeIfAbsent(key, ignored -> new RequestCounter());
        synchronized (counter) {
            counter.lastSeenAt = now;
            while (!counter.requestTimes.isEmpty() && counter.requestTimes.peekFirst() <= now - WINDOW_MILLIS) {
                counter.requestTimes.removeFirst();
            }
            counter.requestTimes.addLast(now);
            return counter.requestTimes.size();
        }
    }

    private void blacklistIp(String ip, String method, String path, String endpoint, int count, long nowMillis) {
        blacklistedIps.add(ip);
        LocalDateTime now = LocalDateTime.now();
        IpBlacklist record = new IpBlacklist();
        record.setIpAddress(ip);
        record.setTriggerMethod(method);
        record.setTriggerPath(path);
        record.setTriggerEndpoint(endpoint);
        record.setRequestCount(count);
        record.setWindowStartedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis - WINDOW_MILLIS), ZoneId.systemDefault()));
        record.setReason("同一IP同一接口1秒内请求超过" + properties.getBlacklistRequestsPerSecond() + "次");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            ipBlacklistMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.debug("IP already blacklisted: {}", ip);
        }
    }

    private void cleanupCounters(long now) {
        long lastCleanup = lastCleanupAt.get();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS || !lastCleanupAt.compareAndSet(lastCleanup, now)) {
            return;
        }
        long expiredAt = now - COUNTER_TTL_MILLIS;
        counters.entrySet().removeIf(entry -> entry.getValue().lastSeenAt < expiredAt);
    }

    private String resolveEndpoint(HttpServletRequest request) {
        Object bestMatch = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = bestMatch instanceof String && StringUtils.hasText((String) bestMatch)
                ? (String) bestMatch
                : request.getRequestURI();
        return request.getMethod().toUpperCase(Locale.ROOT) + " " + path;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustProxyHeaders()) {
            String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor;
            }
            String realIp = cleanHeaderValue(request.getHeader("X-Real-IP"));
            if (StringUtils.hasText(realIp)) {
                return realIp;
            }
            String forwarded = cleanHeaderValue(request.getHeader("Forwarded"));
            String forwardedIp = parseForwardedFor(forwarded);
            if (StringUtils.hasText(forwardedIp)) {
                return forwardedIp;
            }
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String cleaned = cleanHeaderValue(part);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }
        }
        return "";
    }

    private String cleanHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim();
        if (!StringUtils.hasText(cleaned) || "unknown".equalsIgnoreCase(cleaned)) {
            return "";
        }
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String parseForwardedFor(String forwarded) {
        if (!StringUtils.hasText(forwarded)) {
            return "";
        }
        String[] segments = forwarded.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("for=")) {
                return cleanHeaderValue(trimmed.substring(4));
            }
        }
        return "";
    }

    private void writeLimited(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new Response<>(ResponseStateFactory.getFail().state(), message)
        ));
    }

    private static class RequestCounter {
        private final Deque<Long> requestTimes = new ArrayDeque<>();

        private volatile long lastSeenAt;
    }
}
