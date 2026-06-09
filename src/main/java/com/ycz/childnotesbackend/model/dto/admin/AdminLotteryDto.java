package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminLotteryDto {

    private Long id;

    private String title;

    private String description;

    private String coverImage;

    private String startTime;

    private String drawTime;

    private Integer costPoints;

    private Integer winnerCount;

    private String status;

    private String publishTime;

    private String createdAt;

    private String updatedAt;

    private List<AdminLotteryPrizeDto> prizes = new ArrayList<>();
}
