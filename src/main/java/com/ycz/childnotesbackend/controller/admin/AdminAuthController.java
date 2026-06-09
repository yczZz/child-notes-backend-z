package com.ycz.childnotesbackend.controller.admin;

import com.ycz.childnotesbackend.filter.AuthTokenFilter;
import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.admin.AdminLoginRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminLoginResponse;
import com.ycz.childnotesbackend.service.AdminAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/api/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Response<AdminLoginResponse> login(@RequestBody(required = false) AdminLoginRequest request) {
        return handle(() -> adminAuthService.login(request));
    }

    @GetMapping("/me")
    public Response<AdminLoginResponse> me() {
        return handle(adminAuthService::currentAdmin);
    }

    @PostMapping("/logout")
    public Response<Void> logout(HttpServletRequest request) {
        adminAuthService.logout((String) request.getAttribute(AuthTokenFilter.TOKEN_ATTRIBUTE));
        return Response.SUCCESS;
    }

    private <T> Response<T> handle(Supplier<T> supplier) {
        try {
            return new Response<>(supplier.get());
        } catch (RuntimeException e) {
            return new Response<>("000520", e.getMessage());
        }
    }
}
