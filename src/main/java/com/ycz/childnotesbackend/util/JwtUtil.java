package com.ycz.childnotesbackend.util;

import com.ycz.childnotesbackend.config.JwtProperties;
import com.ycz.childnotesbackend.model.auth.JwtPayload;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties jwtProperties;

    private final ObjectMapper objectMapper;

    public JwtUtil(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    public String createToken(AppUser user, LocalDateTime expireAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uid", user.getId());
        payload.put("openid", user.getOpenid());
        payload.put("exp", toEpochSecond(expireAt));

        String headerPart = base64Url(writeJson(header));
        String payloadPart = base64Url(writeJson(payload));
        String unsignedToken = headerPart + "." + payloadPart;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public JwtPayload parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token不能为空");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("token格式错误");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSign = sign(unsignedToken);
        if (!constantTimeEquals(expectedSign, parts[2])) {
            throw new IllegalArgumentException("token签名错误");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(base64UrlDecode(parts[1]), Map.class);
            Long expireAt = asLong(payload.get("exp"));
            if (expireAt == null || expireAt < System.currentTimeMillis() / 1000L) {
                throw new IllegalArgumentException("token已过期");
            }

            JwtPayload jwtPayload = new JwtPayload();
            jwtPayload.setUserId(asLong(payload.get("uid")));
            jwtPayload.setOpenid((String) payload.get("openid"));
            jwtPayload.setExpireAt(expireAt);
            return jwtPayload;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("token内容错误", e);
        }
    }

    public LocalDateTime defaultExpireAt() {
        return LocalDateTime.now().plusDays(jwtProperties.getExpireDays());
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JWT序列化失败", e);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("JWT签名失败", e);
        }
    }

    private String base64Url(String content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlDecode(String content) {
        return new String(Base64.getUrlDecoder().decode(content), StandardCharsets.UTF_8);
    }

    private long toEpochSecond(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }
}
