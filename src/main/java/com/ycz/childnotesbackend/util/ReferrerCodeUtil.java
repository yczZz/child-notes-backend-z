package com.ycz.childnotesbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ReferrerCodeUtil {

    private static final String PREFIX = "u_";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${jwt.secret:child-notes-jwt-secret-change-me}")
    private String secret;

    public String encode(Long userId) {
        if (userId == null) {
            return "";
        }
        String id = String.valueOf(userId);
        String payload = id + ":" + sign(id);
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public Long decode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.matches("\\d+")) {
            return Long.valueOf(trimmed);
        }
        if (!trimmed.startsWith(PREFIX)) {
            return null;
        }
        try {
            byte[] raw = Base64.getUrlDecoder().decode(trimmed.substring(PREFIX.length()));
            String payload = new String(raw, StandardCharsets.UTF_8);
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            if (!sign(parts[0]).equals(parts[1])) {
                return null;
            }
            return Long.valueOf(parts[0]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(12, digest.length); i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign referrer code", e);
        }
    }
}
