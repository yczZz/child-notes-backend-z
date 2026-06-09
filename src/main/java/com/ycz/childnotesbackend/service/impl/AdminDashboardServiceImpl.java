package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.mapper.AdminLotteryActivityMapper;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.mapper.BabyMapper;
import com.ycz.childnotesbackend.mapper.BabyMemberMapper;
import com.ycz.childnotesbackend.model.dto.admin.AdminBabyListItemDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminOverviewResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;
import com.ycz.childnotesbackend.model.dto.admin.AdminUserListItemDto;
import com.ycz.childnotesbackend.model.entity.AdminLotteryActivity;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.model.entity.Baby;
import com.ycz.childnotesbackend.model.entity.BabyMember;
import com.ycz.childnotesbackend.service.AdminDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppUserMapper appUserMapper;

    private final BabyMapper babyMapper;

    private final BabyMemberMapper babyMemberMapper;

    private final AdminLotteryActivityMapper adminLotteryActivityMapper;

    public AdminDashboardServiceImpl(AppUserMapper appUserMapper,
                                     BabyMapper babyMapper,
                                     BabyMemberMapper babyMemberMapper,
                                     AdminLotteryActivityMapper adminLotteryActivityMapper) {
        this.appUserMapper = appUserMapper;
        this.babyMapper = babyMapper;
        this.babyMemberMapper = babyMemberMapper;
        this.adminLotteryActivityMapper = adminLotteryActivityMapper;
    }

    @Override
    public AdminOverviewResponse getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        AdminOverviewResponse response = new AdminOverviewResponse();
        response.setTotalUsers(appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()));
        response.setTodayUsers(appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .ge(AppUser::getCreatedAt, todayStart)));
        response.setTotalBabies(babyMapper.selectCount(new LambdaQueryWrapper<Baby>()));
        response.setTodayBabies(babyMapper.selectCount(new LambdaQueryWrapper<Baby>()
                .ge(Baby::getCreatedAt, todayStart)));
        response.setDraftLotteryCount(adminLotteryActivityMapper.selectCount(new LambdaQueryWrapper<AdminLotteryActivity>()
                .eq(AdminLotteryActivity::getStatus, "draft")));
        response.setPublishedLotteryCount(adminLotteryActivityMapper.selectCount(new LambdaQueryWrapper<AdminLotteryActivity>()
                .eq(AdminLotteryActivity::getStatus, "published")));
        return response;
    }

    @Override
    public AdminPageResponse<AdminUserListItemDto> listUsers(int page, int pageSize, String keyword) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Long total = appUserMapper.selectCount(buildUserQuery(keyword));
        List<AppUser> users = appUserMapper.selectList(buildUserQuery(keyword)
                .orderByDesc(AppUser::getCreatedAt)
                .orderByDesc(AppUser::getId)
                .last(limitClause(normalizedPage, normalizedPageSize)));

        AdminPageResponse<AdminUserListItemDto> response = new AdminPageResponse<>();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setRecords(users.stream().map(this::toUserItem).collect(Collectors.toList()));
        return response;
    }

    @Override
    public AdminPageResponse<AdminBabyListItemDto> listBabies(int page, int pageSize, String keyword) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Long total = babyMapper.selectCount(buildBabyQuery(keyword));
        List<Baby> babies = babyMapper.selectList(buildBabyQuery(keyword)
                .orderByDesc(Baby::getCreatedAt)
                .orderByDesc(Baby::getId)
                .last(limitClause(normalizedPage, normalizedPageSize)));

        AdminPageResponse<AdminBabyListItemDto> response = new AdminPageResponse<>();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setRecords(babies.stream().map(this::toBabyItem).collect(Collectors.toList()));
        return response;
    }

    private LambdaQueryWrapper<AppUser> buildUserQuery(String keyword) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AppUser::getNickName, keyword.trim());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Baby> buildBabyQuery(String keyword) {
        LambdaQueryWrapper<Baby> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Baby::getName, keyword.trim());
        }
        return wrapper;
    }

    private AdminUserListItemDto toUserItem(AppUser user) {
        AdminUserListItemDto item = new AdminUserListItemDto();
        item.setId(user.getId());
        item.setNickName(user.getNickName());
        item.setAvatarUrl(user.getAvatarUrl());
        item.setGender(user.getGender());
        item.setReferrerUserId(user.getReferrerUserId());
        item.setBabyCount(babyMemberMapper.selectCount(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, user.getId())
                .eq(BabyMember::getStatus, "active")));
        item.setCreatedAt(formatDateTime(user.getCreatedAt()));
        return item;
    }

    private AdminBabyListItemDto toBabyItem(Baby baby) {
        AppUser owner = baby.getUserId() == null ? null : appUserMapper.selectById(baby.getUserId());
        AdminBabyListItemDto item = new AdminBabyListItemDto();
        item.setId(baby.getId());
        item.setName(baby.getName());
        item.setAvatar(baby.getAvatar());
        item.setGender(baby.getGender());
        item.setBirthDate(baby.getBirthDate() == null ? null : baby.getBirthDate().toString());
        item.setAgeDays(baby.getBirthDate() == null ? null : ChronoUnit.DAYS.between(baby.getBirthDate(), LocalDate.now()));
        item.setOwnerUserId(baby.getUserId());
        item.setOwnerNickName(owner == null ? null : owner.getNickName());
        item.setMemberCount(babyMemberMapper.selectCount(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getBabyId, baby.getId())
                .eq(BabyMember::getStatus, "active")));
        item.setCreatedAt(formatDateTime(baby.getCreatedAt()));
        return item;
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private String limitClause(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return "limit " + offset + ", " + pageSize;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME_FORMATTER);
    }
}
