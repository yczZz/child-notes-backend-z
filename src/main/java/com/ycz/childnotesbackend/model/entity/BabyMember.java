package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("baby_member")
public class BabyMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long babyId;

    private Long userId;

    private String roleCode;

    private String roleName;

    @TableField("is_owner")
    private Boolean owner;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
