package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.context.AdminAuthContext;
import com.ycz.childnotesbackend.mapper.AdminLotteryActivityMapper;
import com.ycz.childnotesbackend.mapper.AdminLotteryPrizeMapper;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryPrizeDto;
import com.ycz.childnotesbackend.model.dto.admin.AdminLotteryRequest;
import com.ycz.childnotesbackend.model.dto.admin.AdminPageResponse;
import com.ycz.childnotesbackend.model.entity.AdminLotteryActivity;
import com.ycz.childnotesbackend.model.entity.AdminLotteryPrize;
import com.ycz.childnotesbackend.service.AdminLotteryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminLotteryServiceImpl implements AdminLotteryService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> ALLOWED_STATUSES = Set.of("draft", "published", "closed");

    private final AdminLotteryActivityMapper adminLotteryActivityMapper;

    private final AdminLotteryPrizeMapper adminLotteryPrizeMapper;

    public AdminLotteryServiceImpl(AdminLotteryActivityMapper adminLotteryActivityMapper,
                                   AdminLotteryPrizeMapper adminLotteryPrizeMapper) {
        this.adminLotteryActivityMapper = adminLotteryActivityMapper;
        this.adminLotteryPrizeMapper = adminLotteryPrizeMapper;
    }

    @Override
    public AdminPageResponse<AdminLotteryDto> listLotteries(int page, int pageSize, String status) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Long total = adminLotteryActivityMapper.selectCount(buildLotteryQuery(status));
        List<AdminLotteryActivity> activities = adminLotteryActivityMapper.selectList(buildLotteryQuery(status)
                .orderByDesc(AdminLotteryActivity::getUpdatedAt)
                .orderByDesc(AdminLotteryActivity::getId)
                .last(limitClause(normalizedPage, normalizedPageSize)));

        AdminPageResponse<AdminLotteryDto> response = new AdminPageResponse<>();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setRecords(activities.stream().map(this::toLotteryDto).collect(Collectors.toList()));
        return response;
    }

    @Override
    public AdminLotteryDto getLottery(Long id) {
        return toLotteryDto(requireLottery(id));
    }

    @Override
    @Transactional
    public AdminLotteryDto createLottery(AdminLotteryRequest request) {
        validateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        String status = normalizeStatus(request.getStatus(), "draft");
        AdminLotteryActivity activity = new AdminLotteryActivity();
        applyRequest(activity, request);
        activity.setStatus(status);
        activity.setPublishTime("published".equals(status) ? now : null);
        activity.setCreatedBy(AdminAuthContext.getCurrentAdminId());
        activity.setUpdatedBy(AdminAuthContext.getCurrentAdminId());
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        adminLotteryActivityMapper.insert(activity);
        replacePrizes(activity.getId(), request.getPrizes(), now);
        return toLotteryDto(activity);
    }

    @Override
    @Transactional
    public AdminLotteryDto updateLottery(Long id, AdminLotteryRequest request) {
        validateRequest(request);
        AdminLotteryActivity activity = requireLottery(id);
        LocalDateTime now = LocalDateTime.now();
        String status = normalizeStatus(request.getStatus(), activity.getStatus());
        applyRequest(activity, request);
        activity.setStatus(status);
        if ("published".equals(status) && activity.getPublishTime() == null) {
            activity.setPublishTime(now);
        }
        activity.setUpdatedBy(AdminAuthContext.getCurrentAdminId());
        activity.setUpdatedAt(now);
        adminLotteryActivityMapper.updateById(activity);
        replacePrizes(activity.getId(), request.getPrizes(), now);
        return toLotteryDto(activity);
    }

    @Override
    public AdminLotteryDto publishLottery(Long id) {
        AdminLotteryActivity activity = requireLottery(id);
        Long prizeCount = adminLotteryPrizeMapper.selectCount(new LambdaQueryWrapper<AdminLotteryPrize>()
                .eq(AdminLotteryPrize::getActivityId, id));
        if (prizeCount == null || prizeCount <= 0) {
            throw new IllegalArgumentException("Please add at least one prize before publishing");
        }
        LocalDateTime now = LocalDateTime.now();
        activity.setStatus("published");
        if (activity.getPublishTime() == null) {
            activity.setPublishTime(now);
        }
        activity.setUpdatedBy(AdminAuthContext.getCurrentAdminId());
        activity.setUpdatedAt(now);
        adminLotteryActivityMapper.updateById(activity);
        return toLotteryDto(activity);
    }

    @Override
    public AdminLotteryDto closeLottery(Long id) {
        AdminLotteryActivity activity = requireLottery(id);
        activity.setStatus("closed");
        activity.setUpdatedBy(AdminAuthContext.getCurrentAdminId());
        activity.setUpdatedAt(LocalDateTime.now());
        adminLotteryActivityMapper.updateById(activity);
        return toLotteryDto(activity);
    }

    private LambdaQueryWrapper<AdminLotteryActivity> buildLotteryQuery(String status) {
        LambdaQueryWrapper<AdminLotteryActivity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(AdminLotteryActivity::getStatus, status.trim());
        }
        return wrapper;
    }

    private AdminLotteryActivity requireLottery(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Lottery id is required");
        }
        AdminLotteryActivity activity = adminLotteryActivityMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("Lottery does not exist");
        }
        return activity;
    }

    private void validateRequest(AdminLotteryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Lottery data is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Lottery title is required");
        }
        if (request.getStartTime() == null || request.getDrawTime() == null) {
            throw new IllegalArgumentException("Start time and draw time are required");
        }
        if (request.getDrawTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Draw time must be after start time");
        }
        String status = normalizeStatus(request.getStatus(), "draft");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unsupported lottery status");
        }
    }

    private void applyRequest(AdminLotteryActivity activity, AdminLotteryRequest request) {
        activity.setTitle(request.getTitle().trim());
        activity.setDescription(request.getDescription());
        activity.setCoverImage(request.getCoverImage());
        activity.setStartTime(request.getStartTime());
        activity.setDrawTime(request.getDrawTime());
        activity.setCostPoints(request.getCostPoints() == null || request.getCostPoints() < 0 ? 0 : request.getCostPoints());
        activity.setWinnerCount(request.getWinnerCount() == null || request.getWinnerCount() <= 0 ? 1 : request.getWinnerCount());
    }

    private void replacePrizes(Long activityId, List<AdminLotteryPrizeDto> prizes, LocalDateTime now) {
        adminLotteryPrizeMapper.delete(new LambdaQueryWrapper<AdminLotteryPrize>()
                .eq(AdminLotteryPrize::getActivityId, activityId));
        List<AdminLotteryPrizeDto> safePrizes = prizes == null ? Collections.emptyList() : prizes;
        for (AdminLotteryPrizeDto prizeDto : safePrizes) {
            if (prizeDto == null || !StringUtils.hasText(prizeDto.getPrizeName())) {
                continue;
            }
            AdminLotteryPrize prize = new AdminLotteryPrize();
            prize.setActivityId(activityId);
            prize.setPrizeName(prizeDto.getPrizeName().trim());
            prize.setPrizeIntro(prizeDto.getPrizeIntro());
            prize.setPrizeImage(prizeDto.getPrizeImage());
            prize.setPrizeCount(prizeDto.getPrizeCount() == null || prizeDto.getPrizeCount() <= 0 ? 1 : prizeDto.getPrizeCount());
            prize.setCreatedAt(now);
            prize.setUpdatedAt(now);
            adminLotteryPrizeMapper.insert(prize);
        }
    }

    private AdminLotteryDto toLotteryDto(AdminLotteryActivity activity) {
        AdminLotteryDto dto = new AdminLotteryDto();
        dto.setId(activity.getId());
        dto.setTitle(activity.getTitle());
        dto.setDescription(activity.getDescription());
        dto.setCoverImage(activity.getCoverImage());
        dto.setStartTime(formatDateTime(activity.getStartTime()));
        dto.setDrawTime(formatDateTime(activity.getDrawTime()));
        dto.setCostPoints(activity.getCostPoints());
        dto.setWinnerCount(activity.getWinnerCount());
        dto.setStatus(activity.getStatus());
        dto.setPublishTime(formatDateTime(activity.getPublishTime()));
        dto.setCreatedAt(formatDateTime(activity.getCreatedAt()));
        dto.setUpdatedAt(formatDateTime(activity.getUpdatedAt()));
        dto.setPrizes(adminLotteryPrizeMapper.selectList(new LambdaQueryWrapper<AdminLotteryPrize>()
                        .eq(AdminLotteryPrize::getActivityId, activity.getId())
                        .orderByAsc(AdminLotteryPrize::getId))
                .stream()
                .map(this::toPrizeDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private AdminLotteryPrizeDto toPrizeDto(AdminLotteryPrize prize) {
        AdminLotteryPrizeDto dto = new AdminLotteryPrizeDto();
        dto.setId(prize.getId());
        dto.setPrizeName(prize.getPrizeName());
        dto.setPrizeIntro(prize.getPrizeIntro());
        dto.setPrizeImage(prize.getPrizeImage());
        dto.setPrizeCount(prize.getPrizeCount());
        return dto;
    }

    private String normalizeStatus(String status, String fallback) {
        return StringUtils.hasText(status) ? status.trim().toLowerCase() : fallback;
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
