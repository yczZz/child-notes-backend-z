package com.ycz.childnotesbackend.model.auth;

import lombok.Data;

@Data
public class AuthUser {

    private Long id;

    private String openid;

    private String nickName;

    private String avatarUrl;

    private Integer gender;
}
