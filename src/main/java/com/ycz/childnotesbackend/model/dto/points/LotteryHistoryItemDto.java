package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

@Data
public class LotteryHistoryItemDto {

    private Long activityId;

    private String title;

    private String prizeName;

    private Integer costPoints;

    private String status;

    private String joinedAt;

    private String drawTime;
}
