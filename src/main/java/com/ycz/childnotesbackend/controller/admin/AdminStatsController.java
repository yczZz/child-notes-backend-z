package com.ycz.childnotesbackend.controller.admin;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.admin.AdminBabyListItemDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminOverviewResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminUserListItemDto;
import com.ycz.childnotesbackend.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/api")
public class AdminStatsController {

    private final AdminDashboardService adminDashboardService;

    public AdminStatsController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    public Response<AdminOverviewResponse> overview() {
        return handle(adminDashboardService::getOverview);
    }

    @GetMapping("/users")
    public Response<AdminPageResponse<AdminUserListItemDto>> users(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        return handle(() -> adminDashboardService.listUsers(page, pageSize, keyword));
    }

    @GetMapping("/babies")
    public Response<AdminPageResponse<AdminBabyListItemDto>> babies(@RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                                    @RequestParam(required = false) String keyword) {
        return handle(() -> adminDashboardService.listBabies(page, pageSize, keyword));
    }

    private <T> Response<T> handle(Supplier<T> supplier) {
        try {
            return new Response<>(supplier.get());
        } catch (RuntimeException e) {
            return new Response<>("000520", e.getMessage());
        }
    }
}
