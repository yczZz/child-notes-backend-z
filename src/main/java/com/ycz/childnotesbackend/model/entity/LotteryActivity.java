package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lottery_activity")
public class LotteryActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String coverImage;

    private LocalDateTime startTime;

    private LocalDateTime drawTime;

    private Integer costPoints;

    private Integer participantCount;

    private Integer winnerCount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
