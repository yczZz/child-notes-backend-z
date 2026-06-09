package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.points.InviteRecordDto;
import com.ycz.childnotesbackend.model.dto.points.LotteryHistoryItemDto;
import com.ycz.childnotesbackend.model.dto.points.LotterySummaryDto;
import com.ycz.childnotesbackend.model.dto.points.PointsDashboardResponse;
import com.ycz.childnotesbackend.model.dto.points.SignInRuleDto;
import com.ycz.childnotesbackend.service.PointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @GetMapping("/dashboard")
    public Response<PointsDashboardResponse> dashboard() {
        return handle(pointsService::getDashboard);
    }

    @GetMapping("/sign-in-rule")
    public Response<SignInRuleDto> signInRule() {
        return handle(pointsService::getSignInRule);
    }

    @PostMapping("/sign-in")
    public Response<PointsDashboardResponse> signIn() {
        return handle(pointsService::signIn);
    }

    @GetMapping("/lottery/active")
    public Response<LotterySummaryDto> activeLottery() {
        return handle(pointsService::getActiveLottery);
    }

    @PostMapping("/lottery/{activityId}/join")
    public Response<PointsDashboardResponse> joinLottery(@PathVariable Long activityId) {
        return handle(() -> pointsService.joinLottery(activityId));
    }

    @GetMapping("/lottery/history")
    public Response<List<LotteryHistoryItemDto>> lotteryHistory() {
        return handle(pointsService::getLotteryHistory);
    }

    @GetMapping("/invite-records")
    public Response<List<InviteRecordDto>> inviteRecords() {
        return handle(pointsService::getInviteRecords);
    }

    private <T> Response<T> handle(Supplier<T> supplier) {
        try {
            return new Response<>(supplier.get());
        } catch (ResponseStatusException e) {
            return new Response<>("000520", e.getReason());
        } catch (RuntimeException e) {
            return new Response<>("000520", e.getMessage());
        }
    }
}
