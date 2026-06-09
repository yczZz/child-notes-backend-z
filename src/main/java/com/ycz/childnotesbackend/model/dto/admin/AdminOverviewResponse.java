package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminOverviewResponse {

    private Long totalUsers;

    private Long todayUsers;

    private Long totalBabies;

    private Long todayBabies;

    private Long draftLotteryCount;

    private Long publishedLotteryCount;
}
