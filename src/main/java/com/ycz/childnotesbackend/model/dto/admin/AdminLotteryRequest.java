package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminLotteryRequest {

    private String title;

    private String description;

    private String coverImage;

    private LocalDateTime startTime;

    private LocalDateTime drawTime;

    private Integer costPoints;

    private Integer winnerCount;

    private String status;

    private List<AdminLotteryPrizeDto> prizes = new ArrayList<>();
}
