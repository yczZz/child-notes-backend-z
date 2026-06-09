package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_custom_vaccine")
public class UserCustomVaccine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long babyId;

    private String name;

    private String category;

    private String disease;

    private String doseLabel;

    private String ageLabel;

    private Integer dueDays;

    private Integer dueWeeks;

    private Integer dueMonths;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
