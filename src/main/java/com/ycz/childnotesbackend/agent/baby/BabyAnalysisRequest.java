package com.ycz.childnotesbackend.agent.baby;

import java.time.LocalDate;

public class BabyAnalysisRequest {

    private final Long userId;

    private final Long babyId;

    private final String babyName;

    private final LocalDate rangeStartDate;

    private final LocalDate rangeEndDate;

    private final String skillPrompt;

    private final String sourceText;

    public BabyAnalysisRequest(Long userId,
                               Long babyId,
                               String babyName,
                               LocalDate rangeStartDate,
                               LocalDate rangeEndDate,
                               String skillPrompt,
                               String sourceText) {
        this.userId = userId;
        this.babyId = babyId;
        this.babyName = babyName;
        this.rangeStartDate = rangeStartDate;
        this.rangeEndDate = rangeEndDate;
        this.skillPrompt = skillPrompt;
        this.sourceText = sourceText;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBabyId() {
        return babyId;
    }

    public String getBabyName() {
        return babyName;
    }

    public LocalDate getRangeStartDate() {
        return rangeStartDate;
    }

    public LocalDate getRangeEndDate() {
        return rangeEndDate;
    }

    public String getSkillPrompt() {
        return skillPrompt;
    }

    public String getSourceText() {
        return sourceText;
    }
}
