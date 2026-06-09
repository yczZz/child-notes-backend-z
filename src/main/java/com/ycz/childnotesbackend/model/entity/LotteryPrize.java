package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lottery_prize")
public class LotteryPrize {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private String prizeName;

    private String prizeIntro;

    private String prizeImage;

    private Integer prizeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
