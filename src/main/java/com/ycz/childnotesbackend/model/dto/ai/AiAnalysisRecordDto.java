package com.ycz.childnotesbackend.model.dto.ai;

import lombok.Data;

@Data
public class AiAnalysisRecordDto {

    private Long id;

    private Long babyId;

    private String babyName;

    private String rangeStartDate;

    private String rangeEndDate;

    private String analysisText;

    private String dataQualityTip;

    private String model;

    private String createdAt;

    private String updatedAt;
}
