package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_supplement_item")
public class UserSupplementItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String itemType;

    private String name;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
