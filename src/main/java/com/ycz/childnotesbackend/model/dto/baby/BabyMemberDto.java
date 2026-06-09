package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class BabyMemberDto {

    private Long memberId;

    private Long babyId;

    private Long userId;

    private String nickName;

    private String avatarUrl;

    private String roleCode;

    private String roleName;

    private Boolean owner;

    private Boolean mine;

    private String lastLoginTime;
}
