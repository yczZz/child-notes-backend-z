package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_points")
public class UserPoints {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long points;

    private Long totalEarned;

    private Long totalSpent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
