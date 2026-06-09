package com.ycz.childnotesbackend.agent.baby;

import com.ycz.childnotesbackend.config.BabyAnalysisAgentProperties;
import com.ycz.childnotesbackend.config.DeepSeekProperties;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentScopeBabyAnalysisAgent implements BabyAnalysisAgent {

    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

    private final DeepSeekProperties deepSeekProperties;

    private final BabyAnalysisAgentProperties agentProperties;

    private final GenerateOptions generateOptions;

    private final Model model;

    public AgentScopeBabyAnalysisAgent(DeepSeekProperties deepSeekProperties,
                                       BabyAnalysisAgentProperties agentProperties) {
        this.deepSeekProperties = deepSeekProperties;
        this.agentProperties = agentProperties;
        this.generateOptions = buildGenerateOptions();
        this.model = buildModel();
    }

    @Override
    public BabyAnalysisResult analyze(BabyAnalysisRequest request) {
        validate(request);
        String userScopeId = userScopeId(request.getUserId());
        String agentId = agentId(userScopeId);
        String sessionId = sessionId(userScopeId, request.getBabyId());
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(userScopeId)
                .sessionId(sessionId)
                .put("babyId", request.getBabyId())
                .put("babyName", request.getBabyName())
                .put("rangeStartDate", request.getRangeStartDate())
                .put("rangeEndDate", request.getRangeEndDate())
                .build();

        try (HarnessAgent agent = buildAgent(request, agentId, userScopeId)) {
            Msg response = agent.call(new UserMessage(buildUserPrompt(request)), runtimeContext)
                    .block(Duration.ofSeconds(timeoutSeconds()));
            String analysisText = response == null ? null : response.getTextContent();
            if (!StringUtils.hasText(analysisText)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Baby analysis agent response is empty");
            }
            return new BabyAnalysisResult(analysisText.trim(), deepSeekProperties.getModel(), agentId, sessionId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalStateException e) {
            if (StringUtils.hasText(e.getMessage()) && e.getMessage().contains("Timeout")) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Baby analysis agent timed out", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Baby analysis agent call failed", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Baby analysis agent call failed", e);
        }
    }

    private HarnessAgent buildAgent(BabyAnalysisRequest request, String agentId, String userScopeId) {
        return HarnessAgent.builder()
                .agentId(agentId)
                .name(agentProperties.getName())
                .description(agentProperties.getDescription())
                .sysPrompt(request.getSkillPrompt())
                .model(model)
                .generateOptions(generateOptions)
                .maxIters(maxIters())
                .workspace(workspaceFor(userScopeId))
                .disableFilesystemTools()
                .disableShellTool()
                .disableSubagents()
                .disableDynamicSubagents()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .disableToolsConfig()
                .disableMemoryTools()
                .build();
    }

    private Model buildModel() {
        return OpenAIChatModel.builder()
                .apiKey(deepSeekProperties.getApiKey())
                .baseUrl(trimTrailingSlash(deepSeekProperties.getBaseUrl()))
                .endpointPath(CHAT_COMPLETIONS_ENDPOINT)
                .modelName(deepSeekProperties.getModel())
                .stream(false)
                .formatter(new DeepSeekFormatter())
                .generateOptions(generateOptions)
                .build();
    }

    private GenerateOptions buildGenerateOptions() {
        GenerateOptions.Builder builder = GenerateOptions.builder()
                .stream(false)
                .temperature(deepSeekProperties.getTemperature())
                .maxTokens(deepSeekProperties.getMaxTokens())
                .additionalBodyParam("thinking", thinkingOptions());
        if (Boolean.TRUE.equals(deepSeekProperties.getThinkingEnabled())
                && StringUtils.hasText(deepSeekProperties.getReasoningEffort())) {
            builder.reasoningEffort(deepSeekProperties.getReasoningEffort());
        }
        return builder.build();
    }

    private Map<String, Object> thinkingOptions() {
        Map<String, Object> thinking = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(deepSeekProperties.getThinkingEnabled())) {
            thinking.put("type", "enabled");
            thinking.put("reasoning_effort", deepSeekProperties.getReasoningEffort());
        } else {
            thinking.put("type", "disabled");
        }
        return thinking;
    }

    private String buildUserPrompt(BabyAnalysisRequest request) {
        return "请只基于本次输入 TXT 生成分析，不要引用历史会话中未出现在 TXT 的内容。\n\n"
                + "下面是后端整理的宝宝所选连续7天记录 TXT，请基于这些记录输出分析和建议。\n\n"
                + request.getSourceText();
    }

    private void validate(BabyAnalysisRequest request) {
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DeepSeek API key is not configured");
        }
        if (request == null
                || request.getUserId() == null
                || request.getBabyId() == null
                || !StringUtils.hasText(request.getSkillPrompt())
                || !StringUtils.hasText(request.getSourceText())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Baby analysis agent request is incomplete");
        }
    }

    private Path workspaceFor(String userScopeId) {
        return Paths.get(agentProperties.getWorkspace())
                .resolve("users")
                .resolve(userScopeId);
    }

    private String userScopeId(Long userId) {
        return "u" + userId;
    }

    private String agentId(String userScopeId) {
        return sanitize(agentProperties.getName()) + "-" + userScopeId;
    }

    private String sessionId(String userScopeId, Long babyId) {
        return sanitize(agentProperties.getSessionPrefix()) + ":" + userScopeId + ":b" + babyId;
    }

    private int maxIters() {
        return Math.max(1, agentProperties.getMaxIters());
    }

    private long timeoutSeconds() {
        return Math.max(1, agentProperties.getTimeoutSeconds());
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "baby-analysis";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._:-]", "-");
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "https://api.deepseek.com";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
