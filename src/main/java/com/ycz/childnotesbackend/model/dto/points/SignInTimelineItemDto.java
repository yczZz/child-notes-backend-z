package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

@Data
public class SignInTimelineItemDto {

    private String date;

    private String label;

    private Boolean today = false;

    private Boolean signed = false;

    private Integer rewardPoints = 0;

    private String displayReward = "-";
}
