package com.ycz.childnotesbackend.model.dto.auth;

import lombok.Data;

@Data
public class LoginUserDto {

    private Long id;

    private String openid;

    private String nickName;

    private String avatarUrl;

    private Integer gender;
}
