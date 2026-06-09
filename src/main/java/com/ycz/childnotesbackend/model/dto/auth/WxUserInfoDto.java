package com.ycz.childnotesbackend.model.dto.auth;

import lombok.Data;

@Data
public class WxUserInfoDto {

    private String nickName;

    private String avatarUrl;

    private Integer gender;
}
