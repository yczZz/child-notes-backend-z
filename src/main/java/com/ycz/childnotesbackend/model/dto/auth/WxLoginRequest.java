package com.ycz.childnotesbackend.model.dto.auth;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Data
public class WxLoginRequest {

    @NotBlank(message = "code不能为空")
    private String code;

    @Valid
    private WxUserInfoDto userInfo;

    private String referrerId;
}
