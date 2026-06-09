package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.config.AdminProperties;
import com.ycz.childnotesbackend.context.AdminAuthContext;
import com.ycz.childnotesbackend.mapper.AdminAccountMapper;
import com.ycz.childnotesbackend.model.dto.admin.AdminLoginRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminLoginResponse;
import com.ycz.childnotesbackend.model.entity.AdminAccount;
import com.ycz.childnotesbackend.service.AdminAuthService;
import com.ycz.childnotesbackend.util.AdminPasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdminAccountMapper adminAccountMapper;

    private final AdminProperties adminProperties;

    public AdminAuthServiceImpl(AdminAccountMapper adminAccountMapper, AdminProperties adminProperties) {
        this.adminAccountMapper = adminAccountMapper;
        this.adminProperties = adminProperties;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        ensureDefaultAdmin();
        String username = request == null ? "" : request.getUsername();
        String password = request == null ? "" : request.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Please enter username and password");
        }

        AdminAccount account = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getUsername, username.trim())
                .last("limit 1"));
        if (account == null || !"active".equals(account.getStatus())
                || !AdminPasswordUtil.matches(password, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        account.setToken(token);
        account.setTokenExpireAt(now.plusHours(Math.max(1L, adminProperties.getTokenExpireHours())));
        account.setLastLoginAt(now);
        account.setUpdatedAt(now);
        adminAccountMapper.updateById(account);
        return toLoginResponse(account);
    }

    @Override
    public AdminLoginResponse currentAdmin() {
        AdminAccount account = AdminAuthContext.getCurrentAdmin();
        return account == null ? null : toLoginResponse(account);
    }

    @Override
    public AdminAccount authenticate(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getToken, token)
                .eq(AdminAccount::getStatus, "active")
                .gt(AdminAccount::getTokenExpireAt, LocalDateTime.now())
                .last("limit 1"));
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        AdminAccount account = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getToken, token)
                .last("limit 1"));
        if (account == null) {
            return;
        }
        account.setToken(null);
        account.setTokenExpireAt(null);
        account.setUpdatedAt(LocalDateTime.now());
        adminAccountMapper.updateById(account);
    }

    private synchronized void ensureDefaultAdmin() {
        Long count = adminAccountMapper.selectCount(new LambdaQueryWrapper<AdminAccount>());
        if (count != null && count > 0) {
            return;
        }
        if (!StringUtils.hasText(adminProperties.getInitPassword())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String salt = AdminPasswordUtil.generateSalt();
        AdminAccount account = new AdminAccount();
        account.setUsername(defaultText(adminProperties.getInitUsername(), "admin"));
        account.setPasswordSalt(salt);
        account.setPasswordHash(AdminPasswordUtil.hash(adminProperties.getInitPassword().trim(), salt));
        account.setDisplayName(defaultText(adminProperties.getInitDisplayName(), "Administrator"));
        account.setStatus("active");
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        adminAccountMapper.insert(account);
    }

    private AdminLoginResponse toLoginResponse(AdminAccount account) {
        AdminLoginResponse response = new AdminLoginResponse();
        response.setAdminId(account.getId());
        response.setUsername(account.getUsername());
        response.setDisplayName(account.getDisplayName());
        response.setToken(account.getToken());
        response.setTokenExpireAt(formatDateTime(account.getTokenExpireAt()));
        return response;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME_FORMATTER);
    }
}
