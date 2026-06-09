package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SignInSummaryDto {

    private Boolean todaySigned = false;

    private Integer continuousDays = 0;

    private Integer todayRewardPoints = 1;

    private Integer nextRewardPoints = 1;

    private SignInRuleDto rule;

    private List<SignInTimelineItemDto> timeline = new ArrayList<>();
}
