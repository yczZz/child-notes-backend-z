package com.ycz.childnotesbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private int maxRequestsPerSecond = 5;

    private int blacklistRequestsPerSecond = 10;

    private boolean trustProxyHeaders = true;
}
