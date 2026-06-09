package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String unionid;

    private String sessionKey;

    private String nickName;

    private String avatarUrl;

    private Integer gender;

    private Long referrerUserId;

    private LocalDateTime referrerBoundAt;

    private String token;

    private LocalDateTime tokenExpireAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
