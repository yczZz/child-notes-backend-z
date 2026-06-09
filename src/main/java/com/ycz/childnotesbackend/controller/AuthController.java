package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.auth.LoginUserDto;
import com.ycz.childnotesbackend.model.dto.auth.UpdateProfileRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginRequest;
import com.ycz.childnotesbackend.model.dto.auth.WxLoginResponse;
import com.ycz.childnotesbackend.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/wx-login")
    public Response<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        try {

            return new Response<>(authService.wxLogin(request));
        }catch (Exception e){
            log.error("wx-login error",e);
            return new Response<>("500",e.getMessage());
        }

    }

    @GetMapping("/me")
    public Response<LoginUserDto> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return new Response<>(authService.getUserByToken(authorization));
    }

    @PutMapping("/profile")
    public Response<LoginUserDto> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateProfileRequest request) {
        return new Response<>(authService.updateProfile(authorization, request));
    }
}
