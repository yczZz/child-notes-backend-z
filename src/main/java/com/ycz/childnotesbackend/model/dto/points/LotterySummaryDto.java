package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LotterySummaryDto {

    private Long activityId;

    private String title;

    private String description;

    private String drawTime;

    private Integer costPoints;

    private Integer participantCount;

    private Integer winnerCount;

    private Boolean alreadyJoined = false;

    private String prizeName;

    private String prizeIntro;

    private String prizeImage;

    private List<String> participantAvatars = new ArrayList<>();
}
