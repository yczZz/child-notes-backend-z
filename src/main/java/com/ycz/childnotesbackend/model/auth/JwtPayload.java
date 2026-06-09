package com.ycz.childnotesbackend.model.auth;

import lombok.Data;

@Data
public class JwtPayload {

    private Long userId;

    private String openid;

    private Long expireAt;
}
