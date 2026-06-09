package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SignInRuleDto {

    private Integer cycleDays = 30;

    private Integer baseReward = 1;

    private String description;

    private List<SignInRewardRuleDto> rewards = new ArrayList<>();
}
