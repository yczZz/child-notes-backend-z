package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_account")
public class AdminAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordSalt;

    private String passwordHash;

    private String displayName;

    private String status;

    private String token;

    private LocalDateTime tokenExpireAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
