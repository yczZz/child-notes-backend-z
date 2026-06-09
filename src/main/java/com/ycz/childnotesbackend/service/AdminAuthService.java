package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.admin.AdminLoginRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminLoginResponse;
import com.ycz.childnotesbackend.model.entity.AdminAccount;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);

    AdminLoginResponse currentAdmin();

    AdminAccount authenticate(String token);

    void logout(String token);
}
