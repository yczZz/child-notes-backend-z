package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("child_note")
public class ChildNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String childName;

    private String title;

    private String content;

    private LocalDate noteDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
