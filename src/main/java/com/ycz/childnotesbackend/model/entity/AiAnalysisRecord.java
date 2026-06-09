package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long babyId;

    private String babyName;

    private LocalDate rangeStartDate;

    private LocalDate rangeEndDate;

    private String sourceText;

    private String skillPrompt;

    private String analysisText;

    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
