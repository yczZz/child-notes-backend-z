package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class BabyGrowthStageDto {

    private Long id;

    private Integer startDay;

    private Integer endDay;

    private String stageName;

    private String subtitle;

    private String physicalChanges;
}
