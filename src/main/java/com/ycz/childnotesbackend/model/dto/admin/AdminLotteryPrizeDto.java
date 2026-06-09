package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminLotteryPrizeDto {

    private Long id;

    private String prizeName;

    private String prizeIntro;

    private String prizeImage;

    private Integer prizeCount;
}
