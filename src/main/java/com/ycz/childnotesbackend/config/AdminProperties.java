package com.ycz.childnotesbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private String initUsername = "admin";

    private String initPassword = "";

    private String initDisplayName = "Administrator";

    private long tokenExpireHours = 12L;
}
