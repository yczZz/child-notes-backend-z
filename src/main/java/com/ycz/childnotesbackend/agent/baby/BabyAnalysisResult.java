package com.ycz.childnotesbackend.agent.baby;

public class BabyAnalysisResult {

    private final String analysisText;

    private final String model;

    private final String agentId;

    private final String sessionId;

    public BabyAnalysisResult(String analysisText, String model, String agentId, String sessionId) {
        this.analysisText = analysisText;
        this.model = model;
        this.agentId = agentId;
        this.sessionId = sessionId;
    }

    public String getAnalysisText() {
        return analysisText;
    }

    public String getModel() {
        return model;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
