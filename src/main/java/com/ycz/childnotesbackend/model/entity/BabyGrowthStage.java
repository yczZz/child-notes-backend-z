package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("baby_growth_stage")
public class BabyGrowthStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer startDay;

    private Integer endDay;

    private String stageName;

    private String subtitle;

    private String physicalChanges;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
