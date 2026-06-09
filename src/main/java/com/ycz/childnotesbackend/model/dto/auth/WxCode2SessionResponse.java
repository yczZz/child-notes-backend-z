package com.ycz.childnotesbackend.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WxCode2SessionResponse {

    private String openid;

    private String unionid;

    @JsonProperty("session_key")
    private String sessionKey;

    private Integer errcode;

    private String errmsg;
}
