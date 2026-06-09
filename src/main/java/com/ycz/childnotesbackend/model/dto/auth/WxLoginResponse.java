package com.ycz.childnotesbackend.model.dto.auth;

import lombok.Data;

@Data
public class WxLoginResponse {

    private String token;

    private String expiresAt;

    private LoginUserDto userInfo;

    private Boolean newUser;
}
