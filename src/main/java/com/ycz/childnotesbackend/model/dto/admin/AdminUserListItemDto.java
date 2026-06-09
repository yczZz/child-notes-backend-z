package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminUserListItemDto {

    private Long id;

    private String nickName;

    private String avatarUrl;

    private Integer gender;

    private Long referrerUserId;

    private Long babyCount;

    private String createdAt;
}
