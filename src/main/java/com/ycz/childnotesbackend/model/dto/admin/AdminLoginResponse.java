package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminLoginResponse {

    private Long adminId;

    private String username;

    private String displayName;

    private String token;

    private String tokenExpireAt;
}
