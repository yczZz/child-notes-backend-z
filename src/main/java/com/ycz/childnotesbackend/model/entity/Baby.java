package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("baby")
public class Baby {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String avatar;

    private String gender;

    private LocalDate birthDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
