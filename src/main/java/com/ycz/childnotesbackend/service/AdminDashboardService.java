package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.admin.AdminBabyListItemDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminOverviewResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminUserListItemDto;

public interface AdminDashboardService {

    AdminOverviewResponse getOverview();

    AdminPageResponse<AdminUserListItemDto> listUsers(int page, int pageSize, String keyword);

    AdminPageResponse<AdminBabyListItemDto> listBabies(int page, int pageSize, String keyword);
}
