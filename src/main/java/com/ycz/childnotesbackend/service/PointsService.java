package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.points.InviteRecordDto;
import com.ycz.childnotesbackend.model.dto.points.LotteryHistoryItemDto;
import com.ycz.childnotesbackend.model.dto.points.LotterySummaryDto;
import com.ycz.childnotesbackend.model.dto.points.PointsDashboardResponse;
import com.ycz.childnotesbackend.model.dto.points.SignInRuleDto;

import java.util.List;

public interface PointsService {

    /**
     * 获取积分仪表盘数据
     * Get points dashboard data
     *
     * @return 积分仪表盘响应 / points dashboard response
     */
    PointsDashboardResponse getDashboard();

    /**
     * 获取签到规则
     * Get sign-in rule
     *
     * @return 签到规则 / sign-in rule
     */
    SignInRuleDto getSignInRule();

    /**
     * 执行签到
     * Perform sign-in
     *
     * @return 签到后的仪表盘数据 / dashboard data after sign-in
     */
    PointsDashboardResponse signIn();

    /**
     * 获取当前活跃的抽奖活动
     * Get active lottery activity
     *
     * @return 抽奖活动摘要 / lottery activity summary
     */
    LotterySummaryDto getActiveLottery();

    /**
     * 参与抽奖
     * Join lottery activity
     *
     * @param activityId 活动ID / activity ID
     * @return 参与后的仪表盘数据 / dashboard data after joining
     */
    PointsDashboardResponse joinLottery(Long activityId);

    /**
     * 获取抽奖历史记录
     * Get lottery history
     *
     * @return 抽奖历史列表 / lottery history list
     */
    List<LotteryHistoryItemDto> getLotteryHistory();

    /**
     * 获取邀请记录
     * Get invite records
     *
     * @return 邀请记录列表 / invite records list
     */
    List<InviteRecordDto> getInviteRecords();

    /**
     * 绑定推荐人
     * Bind referrer
     *
     * @param userId    用户ID / user ID
     * @param referrerId 推荐人ID / referrer ID
     * @param newUser   是否为新用户 / whether is a new user
     */
    void bindReferrer(Long userId, String referrerId, boolean newUser);
}
