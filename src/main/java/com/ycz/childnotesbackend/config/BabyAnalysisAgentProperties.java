package com.ycz.childnotesbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baby-analysis.agent")
public class BabyAnalysisAgentProperties {

    private String name = "baby-analysis-agent";

    private String description = "Baby feeding and growth analysis agent";

    private String workspace = ".agentscope/baby-analysis";

    private String sessionPrefix = "baby-analysis";

    private int maxIters = 1;

    private int timeoutSeconds = 120;
}
