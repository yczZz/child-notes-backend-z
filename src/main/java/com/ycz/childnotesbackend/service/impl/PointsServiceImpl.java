package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.mapper.LotteryActivityMapper;
import com.ycz.childnotesbackend.mapper.LotteryParticipationMapper;
import com.ycz.childnotesbackend.mapper.LotteryPrizeMapper;
import com.ycz.childnotesbackend.mapper.SignInRecordMapper;
import com.ycz.childnotesbackend.mapper.TaskRecordMapper;
import com.ycz.childnotesbackend.mapper.UserPointsMapper;
import com.ycz.childnotesbackend.model.dto.points.InviteRecordDto;
import com.ycz.childnotesbackend.model.dto.points.LotteryHistoryItemDto;
import com.ycz.childnotesbackend.model.dto.points.LotterySummaryDto;
import com.ycz.childnotesbackend.model.dto.points.PointsDashboardResponse;
import com.ycz.childnotesbackend.model.dto.points.SignInRewardRuleDto;
import com.ycz.childnotesbackend.model.dto.points.SignInRuleDto;
import com.ycz.childnotesbackend.model.dto.points.SignInSummaryDto;
import com.ycz.childnotesbackend.model.dto.points.SignInTimelineItemDto;
import com.ycz.childnotesbackend.model.dto.points.TaskTemplateDto;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.model.entity.LotteryActivity;
import com.ycz.childnotesbackend.model.entity.LotteryParticipation;
import com.ycz.childnotesbackend.model.entity.LotteryPrize;
import com.ycz.childnotesbackend.model.entity.SignInRecord;
import com.ycz.childnotesbackend.model.entity.TaskRecord;
import com.ycz.childnotesbackend.model.entity.UserPoints;
import com.ycz.childnotesbackend.service.PointsService;
import com.ycz.childnotesbackend.util.ReferrerCodeUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PointsServiceImpl implements PointsService {

    private static final int SIGN_IN_CYCLE_DAYS = 30;
    private static final int BASE_SIGN_IN_REWARD = 1;
    private static final int INVITE_REWARD_POINTS = 100;
    private static final String TASK_INVITE_REGISTER = "invite_register";
    private static final String TASK_INVITE_MOM = "invite_mom";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final UserPointsMapper userPointsMapper;
    private final SignInRecordMapper signInRecordMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final LotteryActivityMapper lotteryActivityMapper;
    private final LotteryPrizeMapper lotteryPrizeMapper;
    private final LotteryParticipationMapper lotteryParticipationMapper;
    private final AppUserMapper appUserMapper;
    private final ReferrerCodeUtil referrerCodeUtil;

    public PointsServiceImpl(UserPointsMapper userPointsMapper,
                             SignInRecordMapper signInRecordMapper,
                             TaskRecordMapper taskRecordMapper,
                             LotteryActivityMapper lotteryActivityMapper,
                             LotteryPrizeMapper lotteryPrizeMapper,
                             LotteryParticipationMapper lotteryParticipationMapper,
                             AppUserMapper appUserMapper,
                             ReferrerCodeUtil referrerCodeUtil) {
        this.userPointsMapper = userPointsMapper;
        this.signInRecordMapper = signInRecordMapper;
        this.taskRecordMapper = taskRecordMapper;
        this.lotteryActivityMapper = lotteryActivityMapper;
        this.lotteryPrizeMapper = lotteryPrizeMapper;
        this.lotteryParticipationMapper = lotteryParticipationMapper;
        this.appUserMapper = appUserMapper;
        this.referrerCodeUtil = referrerCodeUtil;
    }

    /**
     * 获取当前用户的积分仪表盘数据
     * Get points dashboard data for the current user
     * <p>
     * 包含：当前积分、总获得、总消耗、分享推荐码、签到摘要、抽奖活动、任务列表、邀请记录
     * Includes: current points, total earned/spent, share code, sign-in summary, lottery, tasks, invite records
     *
     * @return 积分仪表盘响应 / points dashboard response
     */
    @Override
    public PointsDashboardResponse getDashboard() {
        Long userId = AuthContext.requireCurrentUserId();
        UserPoints points = ensureUserPoints(userId);

        PointsDashboardResponse response = new PointsDashboardResponse();
        response.setPoints(safeLong(points.getPoints()));
        response.setTotalEarned(safeLong(points.getTotalEarned()));
        response.setTotalSpent(safeLong(points.getTotalSpent()));
        response.setShareReferrerId(referrerCodeUtil.encode(userId));
        response.setSignIn(buildSignInSummary(userId));
        response.setLottery(buildActiveLottery(userId));
        response.setTasks(buildTaskTemplates());
        response.setInviteRecords(buildInviteRecords(userId));
        return response;
    }

    /**
     * 获取签到规则说明
     * Get sign-in reward rule configuration
     * <p>
     * 说明签到周期天数、基础奖励和各节点奖励分层细则
     * Describes cycle days, base reward and tiered rewards for milestone days
     *
     * @return 签到规则 DTO / sign-in rule DTO
     */
    @Override
    public SignInRuleDto getSignInRule() {
        SignInRuleDto rule = new SignInRuleDto();
        rule.setCycleDays(SIGN_IN_CYCLE_DAYS);
        rule.setBaseReward(BASE_SIGN_IN_REWARD);
        rule.setDescription("每日可签到一次；连续第3/5/7/30天分别奖励3/5/7/30积分，30天后进入下一轮循环。");
        rule.getRewards().add(buildRewardRule(1, 1, "基础奖励"));
        rule.getRewards().add(buildRewardRule(3, 3, "连续第3天"));
        rule.getRewards().add(buildRewardRule(5, 5, "连续第5天"));
        rule.getRewards().add(buildRewardRule(7, 7, "连续第7天"));
        rule.getRewards().add(buildRewardRule(30, 30, "连续第30天"));
        return rule;
    }

    /**
     * 执行当日签到并发放积分（事务性）
     * Perform today's sign-in and grant reward points (transactional)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 检查今日是否已签到，重复签到抛出 400
     *    Check if already signed in today; throw 400 if already signed
     * 2. 查询上次签到记录，计算连续签到天数
     *    Query last sign-in record, calculate continuous sign-in days
     * 3. 转换为周期内天数（每 30 天一周期），计算奖励积分
     *    Convert to cycle day (30-day cycle), calculate reward points
     * 4. 创建签到记录并持久化
     *    Create sign-in record and persist to DB
     * 5. 增加用户积分并返回更新后的仪表盘数据
     *    Increment user points and return updated dashboard
     *
     * @return 签到后的仪表盘数据 / dashboard data after sign-in
     */
    @Override
    @Transactional
    public PointsDashboardResponse signIn() {
        Long userId = AuthContext.requireCurrentUserId();
        LocalDate today = LocalDate.now();
        Long signedCount = signInRecordMapper.selectCount(new LambdaQueryWrapper<SignInRecord>()
                .eq(SignInRecord::getUserId, userId)
                .eq(SignInRecord::getSignDate, today));
        if (signedCount != null && signedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "今日已签到");
        }

        SignInRecord last = signInRecordMapper.selectOne(new LambdaQueryWrapper<SignInRecord>()
                .eq(SignInRecord::getUserId, userId)
                .orderByDesc(SignInRecord::getSignDate)
                .orderByDesc(SignInRecord::getId)
                .last("limit 1"));

        int continuousDays = last != null && today.minusDays(1).equals(last.getSignDate())
                ? safeInt(last.getContinuousDays()) + 1
                : 1;
        int cycleDay = toCycleDay(continuousDays);
        int rewardPoints = calculateSignInReward(cycleDay);
        LocalDateTime now = LocalDateTime.now();

        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        record.setSignTime(now);
        record.setContinuousDays(continuousDays);
        record.setCycleDay(cycleDay);
        record.setRewardPoints(rewardPoints);
        record.setCreatedAt(now);
        signInRecordMapper.insert(record);

        changePoints(userId, rewardPoints);
        return getDashboard();
    }

    /**
     * 获取当前活跃中的抽奖活动摘要
     * Get summary of the current active lottery activity
     * <p>
     * 如果没有活跃中的抽奖活动则返回 null
     * Returns null if no active lottery activity exists
     *
     * @return 抽奖活动摘要 DTO，无活动时返回 null / lottery summary DTO, null if no active activity
     */
    @Override
    public LotterySummaryDto getActiveLottery() {
        Long userId = AuthContext.requireCurrentUserId();
        return buildActiveLottery(userId);
    }

    /**
     * 消耗积分参与抽奖活动（事务性）
     * Spend points to join a lottery activity (transactional)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 查询活动存在且状态为 active，否则抛出 400
     *    Verify activity exists and is active, throw 400 otherwise
     * 2. 检查用户是否已参与过本期抽奖，重复参与抛出 400
     *    Check if user already joined this activity, throw 400 if so
     * 3. 扣除用户积分（不足时抛出 400）
     *    Deduct user points (throw 400 if insufficient)
     * 4. 创建参与记录并更新活动参与人数
     *    Create participation record and increment activity participant count
     * 5. 返回更新后的仪表盘数据
     *    Return updated dashboard data
     *
     * @param activityId 抽奖活动ID / lottery activity ID
     * @return 参与后的仪表盘数据 / dashboard data after joining
     */
    @Override
    @Transactional
    public PointsDashboardResponse joinLottery(Long activityId) {
        Long userId = AuthContext.requireCurrentUserId();
        LotteryActivity activity = lotteryActivityMapper.selectById(activityId);
        if (activity == null || !"active".equals(activity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "抽奖活动不存在或已结束");
        }

        Long joined = lotteryParticipationMapper.selectCount(new LambdaQueryWrapper<LotteryParticipation>()
                .eq(LotteryParticipation::getActivityId, activityId)
                .eq(LotteryParticipation::getUserId, userId));
        if (joined != null && joined > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "您已参与本期抽奖");
        }

        int costPoints = Optional.ofNullable(activity.getCostPoints()).orElse(30);
        changePoints(userId, -costPoints);

        LocalDateTime now = LocalDateTime.now();
        LotteryParticipation participation = new LotteryParticipation();
        participation.setActivityId(activityId);
        participation.setUserId(userId);
        participation.setCostPoints(costPoints);
        participation.setStatus("joined");
        participation.setCreatedAt(now);
        participation.setUpdatedAt(now);
        lotteryParticipationMapper.insert(participation);

        activity.setParticipantCount(safeInt(activity.getParticipantCount()) + 1);
        activity.setUpdatedAt(now);
        lotteryActivityMapper.updateById(activity);
        return getDashboard();
    }

    /**
     * 获取当前用户的抽奖参与历史记录
     * Get the lottery participation history for the current user
     * <p>
     * 按参与时间倒序返回所有参与记录
     * Returns all participation records ordered by time DESC
     *
     * @return 抽奖历史列表 / lottery history list
     */
    @Override
    public List<LotteryHistoryItemDto> getLotteryHistory() {
        Long userId = AuthContext.requireCurrentUserId();
        List<LotteryParticipation> participations = lotteryParticipationMapper.selectList(
                new LambdaQueryWrapper<LotteryParticipation>()
                        .eq(LotteryParticipation::getUserId, userId)
                        .orderByDesc(LotteryParticipation::getCreatedAt)
                        .orderByDesc(LotteryParticipation::getId));
        return participations.stream()
                .map(this::toLotteryHistoryItem)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户的邀请注册记录
     * Get the invite-register records for the current user
     *
     * @return 邀请记录列表 / invite record list
     */
    @Override
    public List<InviteRecordDto> getInviteRecords() {
        return buildInviteRecords(AuthContext.requireCurrentUserId());
    }

    /**
     * 绑定推荐人并向推荐人发放邀请奖励（事务性）
     * Bind referrer for new user and grant invite reward to referrer (transactional)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 如果不是新用户则直接返回，不处理
     *    Return immediately if not a new user
     * 2. 解码推荐码获取推荐人用户ID，验证参数合法性（不能自我邀请）
     *    Decode referrer code to get referrer user ID; validate (cannot self-refer)
     * 3. 查询双方用户，检查被邀请人未被绑定过推荐人
     *    Load both users, verify invited user has no referrer yet
     * 4. 将推荐人 ID 写入被邀请用户的记录
     *    Write referrer user ID into invited user's record
     * 5. 检查推荐人是否已经因该用户获得过奖励（幂等保护）
     *    Check if referrer already received reward for this user (idempotent protection)
     * 6. 创建任务记录并向推荐人发放 100 积分
     *    Create task record and grant 100 points to referrer
     *
     * @param userId     被邀请用户ID / invited user ID
     * @param referrerId 推荐码 / referrer code
     * @param newUser    是否为新用户 / whether is a new user
     */
    @Override
    @Transactional
    public void bindReferrer(Long userId, String referrerId, boolean newUser) {
        if (!newUser) {
            return;
        }
        Long referrerUserId = referrerCodeUtil.decode(referrerId);
        if (userId == null || referrerUserId == null || Objects.equals(userId, referrerUserId)) {
            return;
        }
        AppUser invitedUser = appUserMapper.selectById(userId);
        AppUser referrerUser = appUserMapper.selectById(referrerUserId);
        if (invitedUser == null || referrerUser == null || invitedUser.getReferrerUserId() != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        invitedUser.setReferrerUserId(referrerUserId);
        invitedUser.setReferrerBoundAt(now);
        invitedUser.setUpdatedAt(now);
        appUserMapper.updateById(invitedUser);

        Long existingReward = taskRecordMapper.selectCount(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getTaskType, TASK_INVITE_REGISTER)
                .eq(TaskRecord::getRelatedUserId, userId));
        if (existingReward != null && existingReward > 0) {
            return;
        }

        TaskRecord record = new TaskRecord();
        record.setUserId(referrerUserId);
        record.setTaskType(TASK_INVITE_REGISTER);
        record.setTaskKey(TASK_INVITE_MOM);
        record.setRelatedUserId(userId);
        record.setPoints(INVITE_REWARD_POINTS);
        record.setStatus("completed");
        record.setPayloadJson("{\"source\":\"referrer_id\"}");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        taskRecordMapper.insert(record);
        changePoints(referrerUserId, INVITE_REWARD_POINTS);
    }

    /**
     * 构建签到摘要信息，包含今日签到状态、连续天数、预估奖励和近期签到时间轴
     * Build sign-in summary including today's status, continuous days, estimated reward and recent timeline
     */
    private SignInSummaryDto buildSignInSummary(Long userId) {
        LocalDate today = LocalDate.now();
        SignInRecord todayRecord = signInRecordMapper.selectOne(new LambdaQueryWrapper<SignInRecord>()
                .eq(SignInRecord::getUserId, userId)
                .eq(SignInRecord::getSignDate, today)
                .last("limit 1"));
        SignInRecord lastRecord = signInRecordMapper.selectOne(new LambdaQueryWrapper<SignInRecord>()
                .eq(SignInRecord::getUserId, userId)
                .le(SignInRecord::getSignDate, today)
                .orderByDesc(SignInRecord::getSignDate)
                .orderByDesc(SignInRecord::getId)
                .last("limit 1"));

        int continuousDays = 0;
        boolean todaySigned = todayRecord != null;
        if (todaySigned) {
            continuousDays = safeInt(todayRecord.getContinuousDays());
        } else if (lastRecord != null && today.minusDays(1).equals(lastRecord.getSignDate())) {
            continuousDays = safeInt(lastRecord.getContinuousDays());
        }

        SignInSummaryDto summary = new SignInSummaryDto();
        summary.setTodaySigned(todaySigned);
        summary.setContinuousDays(continuousDays);
        summary.setTodayRewardPoints(todaySigned
                ? safeInt(todayRecord.getRewardPoints())
                : calculateSignInReward(toCycleDay(continuousDays + 1)));
        summary.setNextRewardPoints(calculateSignInReward(toCycleDay(continuousDays + 1)));
        summary.setRule(getSignInRule());
        summary.setTimeline(buildTimeline(userId, today));
        return summary;
    }

    /**
     * 构建签到时间轴：展示今天前后各三天（共 7 天）的签到状态
     * Build sign-in timeline showing the 7-day window (3 days before and after today)
     */
    private List<SignInTimelineItemDto> buildTimeline(Long userId, LocalDate today) {
        LocalDate start = today.minusDays(3);
        LocalDate end = today.plusDays(3);
        List<SignInRecord> records = signInRecordMapper.selectList(new LambdaQueryWrapper<SignInRecord>()
                .eq(SignInRecord::getUserId, userId)
                .between(SignInRecord::getSignDate, start, end));
        Map<LocalDate, SignInRecord> byDate = records.stream()
                .collect(Collectors.toMap(SignInRecord::getSignDate, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<SignInTimelineItemDto> timeline = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            SignInRecord record = byDate.get(cursor);
            SignInTimelineItemDto item = new SignInTimelineItemDto();
            item.setDate(cursor.toString());
            item.setLabel(formatTimelineLabel(cursor, today));
            item.setToday(cursor.equals(today));
            item.setSigned(record != null);
            item.setRewardPoints(record == null ? 0 : safeInt(record.getRewardPoints()));
            item.setDisplayReward(record == null ? "-" : "+" + safeInt(record.getRewardPoints()));
            timeline.add(item);
            cursor = cursor.plusDays(1);
        }
        return timeline;
    }

    /**
     * 查询并构建当前活跃抽奖活动的摘要 DTO
     * Query and build the active lottery activity summary DTO
     * <p>
     * 包含：活动信息、奖品信息、参与人数、头像列表及当前用户是否已参与
     * Includes: activity info, prize info, participant count, avatar list, and user's participation status
     */
    private LotterySummaryDto buildActiveLottery(Long userId) {
        LotteryActivity activity = lotteryActivityMapper.selectOne(new LambdaQueryWrapper<LotteryActivity>()
                .eq(LotteryActivity::getStatus, "active")
                .orderByAsc(LotteryActivity::getDrawTime)
                .orderByDesc(LotteryActivity::getId)
                .last("limit 1"));
        if (activity == null) {
            return null;
        }
        LotteryPrize prize = lotteryPrizeMapper.selectOne(new LambdaQueryWrapper<LotteryPrize>()
                .eq(LotteryPrize::getActivityId, activity.getId())
                .orderByAsc(LotteryPrize::getId)
                .last("limit 1"));
        Long participantCount = lotteryParticipationMapper.selectCount(new LambdaQueryWrapper<LotteryParticipation>()
                .eq(LotteryParticipation::getActivityId, activity.getId()));
        Long joined = lotteryParticipationMapper.selectCount(new LambdaQueryWrapper<LotteryParticipation>()
                .eq(LotteryParticipation::getActivityId, activity.getId())
                .eq(LotteryParticipation::getUserId, userId));

        LotterySummaryDto dto = new LotterySummaryDto();
        dto.setActivityId(activity.getId());
        dto.setTitle(activity.getTitle());
        dto.setDescription(activity.getDescription());
        dto.setDrawTime(formatDateTime(activity.getDrawTime()));
        dto.setCostPoints(Optional.ofNullable(activity.getCostPoints()).orElse(30));
        dto.setParticipantCount(countToInt(participantCount));
        dto.setWinnerCount(Optional.ofNullable(activity.getWinnerCount()).orElse(1));
        dto.setAlreadyJoined(joined != null && joined > 0);
        dto.setPrizeName(prize == null ? activity.getTitle() : prize.getPrizeName());
        dto.setPrizeIntro(prize == null ? activity.getDescription() : prize.getPrizeIntro());
        dto.setPrizeImage(prize == null ? activity.getCoverImage() : prize.getPrizeImage());
        dto.setParticipantAvatars(loadParticipantAvatars(activity.getId()));
        return dto;
    }

    /**
     * 加载最近参与抽奖的用户头像列表（最多 12 个）
     * Load avatar URLs of the most recent lottery participants (up to 12)
     */
    private List<String> loadParticipantAvatars(Long activityId) {
        List<LotteryParticipation> participations = lotteryParticipationMapper.selectList(
                new LambdaQueryWrapper<LotteryParticipation>()
                        .eq(LotteryParticipation::getActivityId, activityId)
                        .orderByDesc(LotteryParticipation::getCreatedAt)
                        .orderByDesc(LotteryParticipation::getId)
                        .last("limit 12"));
        List<Long> userIds = participations.stream()
                .map(LotteryParticipation::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AppUser> users = appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, item -> item));
        return participations.stream()
                .map(item -> users.get(item.getUserId()))
                .filter(Objects::nonNull)
                .map(AppUser::getAvatarUrl)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 构建任务模板列表，目前只包含“邀请宝妈使用”任务
     * Build task template list, currently only containing "invite mom to use" task
     */
    private List<TaskTemplateDto> buildTaskTemplates() {
        TaskTemplateDto invite = new TaskTemplateDto();
        invite.setTaskKey(TASK_INVITE_MOM);
        invite.setTitle("邀请宝妈使用");
        invite.setDescription("赚取100积分");
        invite.setPoints(INVITE_REWARD_POINTS);
        invite.setAction("share");
        return Collections.singletonList(invite);
    }

    /**
     * 构建用户的邀请记录列表，批量加载被邀请用户信息
     * Build the invite record list for the user, batch loading invited user info
     */
    private List<InviteRecordDto> buildInviteRecords(Long userId) {
        List<TaskRecord> records = taskRecordMapper.selectList(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getUserId, userId)
                .eq(TaskRecord::getTaskType, TASK_INVITE_REGISTER)
                .orderByDesc(TaskRecord::getCreatedAt)
                .orderByDesc(TaskRecord::getId));
        List<Long> invitedIds = records.stream()
                .map(TaskRecord::getRelatedUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AppUser> users = invitedIds.isEmpty()
                ? Collections.emptyMap()
                : appUserMapper.selectBatchIds(invitedIds).stream()
                .collect(Collectors.toMap(AppUser::getId, item -> item));
        return records.stream()
                .map(record -> toInviteRecord(record, users.get(record.getRelatedUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 将抽奖参与记录转换为历史条目 DTO，口查询关联的活动和奖品信息
     * Convert lottery participation record to history item DTO, querying activity and prize info
     */
    private LotteryHistoryItemDto toLotteryHistoryItem(LotteryParticipation participation) {
        LotteryActivity activity = lotteryActivityMapper.selectById(participation.getActivityId());
        LotteryPrize prize = lotteryPrizeMapper.selectOne(new LambdaQueryWrapper<LotteryPrize>()
                .eq(LotteryPrize::getActivityId, participation.getActivityId())
                .orderByAsc(LotteryPrize::getId)
                .last("limit 1"));
        LotteryHistoryItemDto dto = new LotteryHistoryItemDto();
        dto.setActivityId(participation.getActivityId());
        dto.setTitle(activity == null ? "积分抽奖" : activity.getTitle());
        dto.setPrizeName(prize == null ? "" : prize.getPrizeName());
        dto.setCostPoints(participation.getCostPoints());
        dto.setStatus(participation.getStatus());
        dto.setJoinedAt(formatDateTime(participation.getCreatedAt()));
        dto.setDrawTime(activity == null ? null : formatDateTime(activity.getDrawTime()));
        return dto;
    }

    /**
     * 将任务记录和被邀请用户信息转换为邀请记录 DTO
     * Convert task record and invited user info to invite record DTO
     */
    private InviteRecordDto toInviteRecord(TaskRecord record, AppUser invitedUser) {
        InviteRecordDto dto = new InviteRecordDto();
        dto.setId(record.getId());
        dto.setInvitedUserId(record.getRelatedUserId());
        dto.setInvitedNickName(invitedUser == null ? "新用户" : invitedUser.getNickName());
        dto.setInvitedAvatarUrl(invitedUser == null ? "" : invitedUser.getAvatarUrl());
        dto.setPoints(record.getPoints());
        dto.setCreatedAt(formatDateTime(record.getCreatedAt()));
        return dto;
    }

    /**
     * 获取用户积分记录，如果不存在则初始化并插入（干幂操作）
     * Get user points record; initialize and insert if not exists (idempotent via DuplicateKeyException)
     */
    private UserPoints ensureUserPoints(Long userId) {
        UserPoints points = userPointsMapper.selectOne(new LambdaQueryWrapper<UserPoints>()
                .eq(UserPoints::getUserId, userId)
                .last("limit 1"));
        if (points != null) {
            return points;
        }
        LocalDateTime now = LocalDateTime.now();
        UserPoints created = new UserPoints();
        created.setUserId(userId);
        created.setPoints(0L);
        created.setTotalEarned(0L);
        created.setTotalSpent(0L);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        try {
            userPointsMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            return userPointsMapper.selectOne(new LambdaQueryWrapper<UserPoints>()
                    .eq(UserPoints::getUserId, userId)
                    .last("limit 1"));
        }
    }

    /**
     * 修改用户积分（正数为增加，负数为扣除）并更新总获得/总消耗统计
     * Change user points by delta (positive=earn, negative=spend) and update total statistics
     * <p>
     * 积分不足时抛出 400。先调用 ensureUserPoints 保证记录存在
     * Throws 400 if points are insufficient. Always calls ensureUserPoints first
     */
    private UserPoints changePoints(Long userId, long delta) {
        UserPoints points = ensureUserPoints(userId);
        long current = safeLong(points.getPoints());
        if (delta < 0 && current < Math.abs(delta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "积分不足");
        }
        points.setPoints(current + delta);
        if (delta > 0) {
            points.setTotalEarned(safeLong(points.getTotalEarned()) + delta);
        } else if (delta < 0) {
            points.setTotalSpent(safeLong(points.getTotalSpent()) + Math.abs(delta));
        }
        points.setUpdatedAt(LocalDateTime.now());
        userPointsMapper.updateById(points);
        return points;
    }

    /**
     * 构建单条签到奖励规则 DTO
     * Build a single sign-in reward rule DTO
     */
    private SignInRewardRuleDto buildRewardRule(int day, int points, String label) {
        SignInRewardRuleDto rule = new SignInRewardRuleDto();
        rule.setDay(day);
        rule.setPoints(points);
        rule.setLabel(label);
        return rule;
    }

    /**
     * 根据周期内天数计算签到奖励积分
     * Calculate sign-in reward points based on cycle day
     * <p>
     * 奖励规则：第 3/5/7/30 天分别奖励 3/5/7/30 分，其他天奖励基础 1 分
     * Rules: days 3/5/7/30 reward 3/5/7/30 pts; other days reward base 1 pt
     */
    private int calculateSignInReward(int cycleDay) {
        if (cycleDay == 30) {
            return 30;
        }
        if (cycleDay == 7) {
            return 7;
        }
        if (cycleDay == 5) {
            return 5;
        }
        if (cycleDay == 3) {
            return 3;
        }
        return BASE_SIGN_IN_REWARD;
    }

    /**
     * 将累积连续天数转换为周期内天数（30 天一周期）
     * Convert cumulative continuous days to cycle day within a 30-day cycle
     */
    private int toCycleDay(int continuousDays) {
        return ((Math.max(1, continuousDays) - 1) % SIGN_IN_CYCLE_DAYS) + 1;
    }

    /**
     * 将日期转换为签到时间轴的显示标签：今天/昨天/明天/具体日期
     * Convert date to timeline label: "今天"/"昨天"/"明天" or formatted date
     */
    private String formatTimelineLabel(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "今天";
        }
        if (date.equals(today.minusDays(1))) {
            return "昨天";
        }
        if (date.equals(today.plusDays(1))) {
            return "明天";
        }
        return date.format(MONTH_DAY_FORMATTER);
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME_FORMATTER);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int countToInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
