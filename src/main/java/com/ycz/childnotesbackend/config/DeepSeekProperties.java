package com.ycz.childnotesbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String baseUrl = "https://api.deepseek.com";

    private String apiKey;

    private String model = "deepseek-v4-flash";

    private Double temperature = 0.3D;

    private Integer maxTokens = 2500;

    private Boolean thinkingEnabled = false;

    private String reasoningEffort = "high";
}
