package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("child_record")
public class ChildRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long babyId;

    private String recordType;

    private String recordSubType;

    private LocalDate recordDate;

    private LocalDateTime recordTime;

    private Integer amountMl;

    private Integer durationSec;

    private Integer leftDurationSec;

    private Integer rightDurationSec;

    private Boolean abnormalFlag;

    private BigDecimal temperatureValue;

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    private String payloadJson;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
