package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.BabyMemberMapper;
import com.ycz.childnotesbackend.mapper.BabyMapper;
import com.ycz.childnotesbackend.mapper.AiAnalysisRecordMapper;
import com.ycz.childnotesbackend.mapper.ChildRecordMapper;
import com.ycz.childnotesbackend.mapper.UserCustomVaccineMapper;
import com.ycz.childnotesbackend.mapper.UserSupplementItemMapper;
import com.ycz.childnotesbackend.model.dto.record.AbnormalRecordDto;
import com.ycz.childnotesbackend.model.dto.record.BaseRecordDto;
import com.ycz.childnotesbackend.model.dto.record.CustomItemsResponse;
import com.ycz.childnotesbackend.model.dto.record.DailyRecordsResponse;
import com.ycz.childnotesbackend.model.dto.record.DailyStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.DiaperRecordDto;
import com.ycz.childnotesbackend.model.dto.record.FeedRecordDto;
import com.ycz.childnotesbackend.model.dto.record.FeverInfoDto;
import com.ycz.childnotesbackend.model.dto.record.GrowthRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MaternalFoodRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MilestoneRecordDto;
import com.ycz.childnotesbackend.model.dto.record.ComplementaryRecordDto;
import com.ycz.childnotesbackend.model.dto.record.CustomVaccineDto;
import com.ycz.childnotesbackend.model.dto.record.PumpRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SleepRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SupplementOptionsResponse;
import com.ycz.childnotesbackend.model.dto.record.SupplementRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TemperatureRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TodayStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.ActivityRecordDto;
import com.ycz.childnotesbackend.model.dto.record.VaccineRecordDto;
import com.ycz.childnotesbackend.model.entity.Baby;
import com.ycz.childnotesbackend.model.entity.BabyMember;
import com.ycz.childnotesbackend.model.entity.AiAnalysisRecord;
import com.ycz.childnotesbackend.model.entity.ChildRecord;
import com.ycz.childnotesbackend.model.entity.UserCustomVaccine;
import com.ycz.childnotesbackend.model.entity.UserSupplementItem;
import com.ycz.childnotesbackend.service.RecordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RecordServiceImpl implements RecordService {

    private static final DateTimeFormatter FRONT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter FRONT_TIME_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DOSE_UNIT_ITEM_TYPE = "dose_unit";

    private static final Set<String> DEFAULT_DOSE_UNITS = Set.of("ml", "粒", "包");

    private static final BigDecimal FEVER_THRESHOLD = new BigDecimal("37.3");

    private static final List<String> HEALTH_TRACKING_RECORD_TYPES = List.of(
            "abnormal",
            "temperature",
            "diaper",
            "supplement",
            "fever_resolved",
            "diarrhea_resolved",
            "abnormal_resolved"
    );

    private static final String[] DAILY_TIPS = {
        "母乳喂养的宝宝每天大便2-5次是正常的哦",
        "宝宝6个月前纯母乳不需要额外喂水",
        "每天给宝宝做抚触，有助于神经系统发育",
        "宝宝的睡眠周期约45分钟，浅睡时别急着抱",
        "维生素D从出生15天开始补充，每天400IU",
        "宝宝的胃容量很小，按需喂养不必焦虑奶量",
        "拍嗝时竖抱15-20分钟，能有效减少溢奶",
        "宝宝哭闹不一定是饿了，先检查尿布和体温",
        "每天让宝宝趴一会儿，锻炼颈部背部力量",
        "母乳妈妈记得补充钙和铁，保证奶水营养",
        "洗澡水温37-38℃最合适，用手肘试温",
        "宝宝的体温在36.5-37.5℃之间都是正常的",
    };

    private final ChildRecordMapper childRecordMapper;

    private final BabyMapper babyMapper;

    private final BabyMemberMapper babyMemberMapper;

    private final UserSupplementItemMapper userSupplementItemMapper;

    private final UserCustomVaccineMapper userCustomVaccineMapper;

    private final AiAnalysisRecordMapper aiAnalysisRecordMapper;

    private final ObjectMapper objectMapper;

    public RecordServiceImpl(ChildRecordMapper childRecordMapper, BabyMapper babyMapper, BabyMemberMapper babyMemberMapper, UserSupplementItemMapper userSupplementItemMapper, UserCustomVaccineMapper userCustomVaccineMapper, AiAnalysisRecordMapper aiAnalysisRecordMapper, ObjectMapper objectMapper) {
        this.childRecordMapper = childRecordMapper;
        this.babyMapper = babyMapper;
        this.babyMemberMapper = babyMemberMapper;
        this.userSupplementItemMapper = userSupplementItemMapper;
        this.userCustomVaccineMapper = userCustomVaccineMapper;
        this.aiAnalysisRecordMapper = aiAnalysisRecordMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取当前登录用户ID
     * Get current logged-in user ID from AuthContext
     */
    private Long currentUserId() {
        return AuthContext.requireCurrentUserId();
    }

    /**
     * 获取当前宝宝 ID：优先使用请求中指定的 babyId，否则取用户第一个宝宝
     * Get current baby ID: prefer requested babyId, otherwise find user's first baby
     * <p>
     * 找不到宝宝时抛出 404
     * Throws 404 if no baby found
     */
    private Long currentBabyId() {
        Long userId = currentUserId();
        Long requestedBabyId = getRequestedBabyId();
        Baby baby = requestedBabyId == null
                ? findFirstBabyForUser(userId)
                : findBabyForUser(requestedBabyId, userId);
        if (baby == null) {
            throw new ResponseStatusException(NOT_FOUND, "Baby not found: " + (requestedBabyId == null ? "current" : requestedBabyId));
        }
        return baby.getId();
    }

    /**
     * 查找用户有访问权的第一个宝宝：优先家庭成员表，其次直接所有宝宝
     * Find the first baby accessible to the user: prefer membership table, fallback to owned babies
     */
    private Baby findFirstBabyForUser(Long userId) {
        BabyMember member = babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getStatus, "active")
                .orderByAsc(BabyMember::getBabyId)
                .last("limit 1"));
        if (member != null) {
            Baby baby = babyMapper.selectById(member.getBabyId());
            if (baby != null) {
                return baby;
            }
        }
        return babyMapper.selectOne(new LambdaQueryWrapper<Baby>()
                .eq(Baby::getUserId, userId)
                .orderByAsc(Baby::getId)
                .last("limit 1"));
    }

    /**
     * 查找用户有权访问的指定宝宝：成员记录存在或用户为宝宝主人
     * Find a specific baby accessible to the user: active membership exists or user is baby owner
     */
    private Baby findBabyForUser(Long babyId, Long userId) {
        Baby baby = babyMapper.selectById(babyId);
        if (baby == null) {
            return null;
        }
        BabyMember member = babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getBabyId, babyId)
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getStatus, "active")
                .last("limit 1"));
        if (member != null || Objects.equals(baby.getUserId(), userId)) {
            return baby;
        }
        return null;
    }

    /**
     * 从 HTTP 请求头 X-Baby-Id 或查询参数 babyId 中解析用户指定的 babyId
     * Parse user-specified babyId from request header X-Baby-Id or query parameter babyId
     */
    private Long getRequestedBabyId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String raw = request.getHeader("X-Baby-Id");
        if (!StringUtils.hasText(raw)) {
            raw = request.getParameter("babyId");
        }
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid babyId: " + raw);
        }
    }

    /**
     * 将当前用户ID 和当前宝宝ID 写入 ChildRecord 实体
     * Assign current user ID and current baby ID into the ChildRecord entity
     */
    private void assignOwnership(ChildRecord record) {
        record.setUserId(currentUserId());
        record.setBabyId(currentBabyId());
    }

    /**
     * 获取当前宝宝今日的所有记录，等同于调用 getRecordsByDate(today)
     * Get all records for the current baby today; equivalent to calling getRecordsByDate(today)
     *
     * @return 今日全量记录 / all records for today
     */
    @Override
    public DailyRecordsResponse getTodayRecords() {
        return getRecordsByDate(LocalDate.now());
    }

    /**
     * 获取今日宝宝综合统计数据
     * Get comprehensive today's statistics for the current baby
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取今日全量记录
     *    Get all today's records
     * 2. 计算最后一次喂养时间及距当前的时长差
     *    Calculate last feed time and time since last feed
     * 3. 汇总今日睡眠总时长（分钟）
     *    Aggregate total sleep duration today (minutes)
     * 4. 识别尚未恢复的发烧、腹泻和其他异常，支持跨天延续和恢复后复发
     *    Detect unresolved fever, diarrhea and other symptoms across days, including relapse after recovery
     * 5. 返回汇总各项统计和每日小贴士的综合 TodayStatsResponse
     *    Return TodayStatsResponse aggregating all stats and daily tips
     *
     * @return 今日综合统计响应 / today stats response
     */
    @Override
    public TodayStatsResponse getTodayStats() {
        DailyRecordsResponse records = getTodayRecords();
        Optional<FeedRecordDto> lastFeed = records.getFeeds().stream()
                .filter(item -> StringUtils.hasText(item.getTime()))
                .max(Comparator.comparing(FeedRecordDto::getTime));
        String lastFeedTime = lastFeed.map(FeedRecordDto::getTime).orElse(null);

        int totalSleepMin = records.getSleeps().stream()
                .map(SleepRecordDto::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        TodayStatsResponse stats = new TodayStatsResponse();
        stats.setLastFeedTime(lastFeedTime);
        stats.setTimeSinceLastFeed(lastFeedTime == null ? "暂无记录" : calcTimeDiff(parseFrontTime(lastFeedTime), LocalDateTime.now()));
        stats.setTodaySleepTotal(totalSleepMin > 0 ? formatMinutes(totalSleepMin) : "暂无记录");
        stats.setTotalSleepMin(totalSleepMin);
        stats.setFeedCount(records.getFeeds().size());
        int totalMilk = records.getFeeds().stream()
                .map(FeedRecordDto::getAmount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        stats.setTotalMilk(totalMilk);
        stats.setSleepCount(records.getSleeps().size());
        stats.setDiaperCount(records.getDiapers().size());
        stats.setWetDiaperCount((int) records.getDiapers().stream()
                .filter(item -> "wet".equals(item.getType()) || "both".equals(item.getType()))
                .count());
        stats.setDirtyDiaperCount((int) records.getDiapers().stream()
                .filter(item -> "dirty".equals(item.getType()) || "both".equals(item.getType()))
                .count());
        GrowthRecordDto latestGrowth = getLatestGrowth();
        if (latestGrowth != null) {
            stats.setLatestHeight(latestGrowth.getHeight());
            stats.setLatestWeight(latestGrowth.getWeight());
            stats.setLatestGrowthTime(latestGrowth.getTime());
        }
        Long babyId = currentBabyId();
        List<ChildRecord> healthRecords = getHealthTrackingRecords(babyId);

        ChildRecord latestFeverResolved = latestRecord(healthRecords, "fever_resolved");
        List<ChildRecord> activeFeverRecords = healthRecords.stream()
                .filter(record -> isAfter(record, latestFeverResolved))
                .filter(this::isFeverRecord)
                .collect(Collectors.toList());
        ChildRecord latestFever = lastRecord(activeFeverRecords);
        stats.setHasFever(latestFever != null);
        stats.setFeverInfo(latestFever == null ? null : toFeverInfo(latestFever));
        stats.setLastMedicineTime(latestMedicineTime(healthRecords));

        ChildRecord latestDiarrheaResolved = latestRecord(healthRecords, "diarrhea_resolved");
        List<ChildRecord> activeDiarrheaRecords = healthRecords.stream()
                .filter(record -> isAfter(record, latestDiarrheaResolved))
                .filter(this::isDiarrheaRecord)
                .collect(Collectors.toList());
        ChildRecord latestDiarrhea = lastRecord(activeDiarrheaRecords);
        stats.setHasDiarrhea(latestDiarrhea != null);
        if (latestDiarrhea != null) {
            List<String> diarrheaTypes = new java.util.ArrayList<>();
            activeDiarrheaRecords.stream()
                    .flatMap(record -> diarrheaTypes(record).stream())
                    .forEach(type -> {
                        if (!diarrheaTypes.contains(type)) {
                            diarrheaTypes.add(type);
                        }
                    });
            stats.setDiarrheaTime(recordTimeText(latestDiarrhea));
            stats.setDiarrheaTypes(String.join("、", diarrheaTypes));
        }

        ChildRecord latestAbnormalResolved = latestRecord(healthRecords, "abnormal_resolved");
        List<ChildRecord> activeOtherAbnormalRecords = healthRecords.stream()
                .filter(record -> isAfter(record, latestAbnormalResolved))
                .filter(this::isOtherAbnormalRecord)
                .collect(Collectors.toList());
        ChildRecord latestOtherAbnormal = lastRecord(activeOtherAbnormalRecords);
        stats.setHasOtherAbnormal(latestOtherAbnormal != null);
        stats.setOtherAbnormalInfo(latestOtherAbnormal == null ? null : toFeverInfo(latestOtherAbnormal));

        stats.setDailyTips(java.util.Arrays.asList(DAILY_TIPS));
        return stats;
    }

    /**
     * 获取指定日期的宝宝每日统计
     * Get daily statistics for the current baby on the specified date
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 查询当天全部记录，按时间升序排列
     *    Query all records for that day, ordered by time ASC
     * 2. 逐条记录分类叠加到统计对象
     *    Accumulate each record by type into the stats object
     *
     * @param date 查询日期 / query date
     * @return 日统计响应 / daily stats response
     */
    @Override
    @Transactional
    public DailyStatsResponse getDailyStats(LocalDate date) {
        Long babyId = currentBabyId();
        splitOpenSleepIfNeeded(babyId, LocalDateTime.now());
        splitCompletedCrossDaySleepsIfNeeded(babyId);
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordDate, date)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));

        DailyStatsResponse stats = new DailyStatsResponse();
        stats.setDate(date.toString());
        records.forEach(record -> addToDailyStats(stats, record));
        return stats;
    }

    /**
     * 获取宝宝在指定日期范围内的每日统计
     * Get per-day statistics for the current baby within the specified date range
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 防张源/目格日期倒置，并限制范围最大 370 天
     *    Swap if reversed; limit range to max 370 days
     * 2. 批量查询该范围内所有记录
     *    Batch query all records in the range
     * 3. 按天初始化 Map 并将记录分散到对应日的统计对象
     *    Initialize map by day and scatter records into corresponding day stats
     * 4. 按日期顺序返回列表
     *    Return list in date order
     *
     * @param startDate 开始日期 / start date
     * @param endDate   结束日期 / end date
     * @return 每日统计列表 / list of daily stats
     */
    @Override
    @Transactional
    public List<DailyStatsResponse> getDailyStatsRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }
        if (startDate.plusDays(370).isBefore(endDate)) {
            startDate = endDate.minusDays(370);
        }

        Long babyId = currentBabyId();
        splitOpenSleepIfNeeded(babyId, LocalDateTime.now());
        splitCompletedCrossDaySleepsIfNeeded(babyId);
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .between(ChildRecord::getRecordDate, startDate, endDate)
                .orderByAsc(ChildRecord::getRecordDate)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));

        Map<LocalDate, DailyStatsResponse> byDate = new LinkedHashMap<>();
        LocalDate day = startDate;
        while (!day.isAfter(endDate)) {
            DailyStatsResponse stats = new DailyStatsResponse();
            stats.setDate(day.toString());
            byDate.put(day, stats);
            day = day.plusDays(1);
        }
        records.forEach(record -> {
            DailyStatsResponse stats = byDate.get(record.getRecordDate());
            if (stats != null) {
                addToDailyStats(stats, record);
            }
        });
        return new java.util.ArrayList<>(byDate.values());
    }

    /**
     * 获取当前宝宝指定日期的所有记录，按时间升序分类返回
     * Get all records for the current baby on the specified date, categorized and ordered by time
     *
     * @param date 查询日期 / query date
     * @return 当日全量分类记录 / daily records categorized by type
     */
    @Override
    @Transactional
    public DailyRecordsResponse getRecordsByDate(LocalDate date) {
        Long babyId = currentBabyId();
        splitOpenSleepIfNeeded(babyId, LocalDateTime.now());
        splitCompletedCrossDaySleepsIfNeeded(babyId);
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordDate, date)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));

        DailyRecordsResponse response = new DailyRecordsResponse();
        response.setDate(date.toString());
        Map<String, ChildRecord> recoveryMarkers = latestRecoveryMarkers(getHealthTrackingRecords(babyId));
        records.forEach(record -> addToDailyResponse(response, record, recoveryMarkers));
        return response;
    }

    /**
     * 获取当前宝宝今天之前的历史记录，按天分组倒序返回
     * Get historical records (before today) for the current baby, grouped by day in descending order
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 查询宝宝今天之前的所有记录，按日期倒序、时间升序
     *    Query all records before today, ordered by date DESC and time ASC
     * 2. 按日期分组，对每组构建 DailyRecordsResponse
     *    Group by date, build DailyRecordsResponse for each group
     * 3. 返回按日期倒序排列的列表
     *    Return list ordered by date DESC
     *
     * @return 历史记录列表（按日分组）/ historical records list (grouped by day)
     */
    @Override
    @Transactional
    public List<DailyRecordsResponse> getHistoryRecords() {
        Long babyId = currentBabyId();
        LocalDate today = LocalDate.now();
        splitOpenSleepIfNeeded(babyId, LocalDateTime.now());
        splitCompletedCrossDaySleepsIfNeeded(babyId);
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .lt(ChildRecord::getRecordDate, today)
                .orderByDesc(ChildRecord::getRecordDate)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));

        Map<LocalDate, List<ChildRecord>> grouped = records.stream()
                .collect(Collectors.groupingBy(ChildRecord::getRecordDate, LinkedHashMap::new, Collectors.toList()));
        Map<String, ChildRecord> recoveryMarkers = latestRecoveryMarkers(getHealthTrackingRecords(babyId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    DailyRecordsResponse day = new DailyRecordsResponse();
                    day.setDate(entry.getKey().toString());
                    entry.getValue().forEach(record -> addToDailyResponse(day, record, recoveryMarkers));
                    return day;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取当前宝宝今日最新的一条喂养记录
     * Get the latest feed record for the current baby today
     *
     * @return 最新喂养记录 DTO，没有则返回 null / latest feed record DTO, null if none
     */
    @Override
    public FeedRecordDto getLatestFeed() {
        Long babyId = currentBabyId();
        ChildRecord record = childRecordMapper.selectOne(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "feed")
                .eq(ChildRecord::getRecordDate, LocalDate.now())
                .orderByDesc(ChildRecord::getRecordTime)
                .orderByDesc(ChildRecord::getId)
                .last("limit 1"));
        if (record == null) return null;
        return readPayloadWithId(record, FeedRecordDto.class);
    }

    /**
     * 获取当前宝宝最新的生长记录（身高和体重分别取最近一条有效值）
     * Get the latest growth record for the current baby
     * (height and weight each taken from the most recent record that has the field set)
     */
    private GrowthRecordDto getLatestGrowth() {
        Long babyId = currentBabyId();
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "growth")
                .orderByDesc(ChildRecord::getRecordTime)
                .orderByDesc(ChildRecord::getId));
        GrowthRecordDto latest = new GrowthRecordDto();
        for (ChildRecord record : records) {
            GrowthRecordDto growth = readPayloadWithId(record, GrowthRecordDto.class);
            if (!StringUtils.hasText(latest.getTime())) {
                latest.setTime(growth.getTime());
            }
            if (latest.getHeight() == null && growth.getHeight() != null) {
                latest.setHeight(growth.getHeight());
            }
            if (latest.getWeight() == null && growth.getWeight() != null) {
                latest.setWeight(growth.getWeight());
            }
            if (latest.getHeight() != null && latest.getWeight() != null) {
                break;
            }
        }
        return latest.getHeight() == null && latest.getWeight() == null ? null : latest;
    }

    /**
     * 添加喂养记录（接母乳/奶瓶/背奔母乳）
     * Add a feed record (breastfeeding / bottle / expressed milk)
     *
     * @param data 喂养记录数据 / feed record data
     * @return 创建后的喂养记录 DTO / created feed record DTO
     */
    @Override
    public FeedRecordDto addFeedRecord(FeedRecordDto data) {
        return addRecord("feed", data, FeedRecordDto::getTime);
    }

    /**
     * 添加换尿布记录
     * Add a diaper change record
     *
     * @param data 换尿布记录数据 / diaper record data
     * @return 创建后的换尿布记录 DTO / created diaper record DTO
     */
    @Override
    public DiaperRecordDto addDiaperRecord(DiaperRecordDto data) {
        return addRecord("diaper", data, DiaperRecordDto::getTime);
    }

    /**
     * 添加睡眠记录
     * Add a sleep record
     *
     * @param data 睡眠记录数据 / sleep record data
     * @return 创建后的睡眠记录 DTO / created sleep record DTO
     */
    @Override
    @Transactional
    public SleepRecordDto addSleepRecord(SleepRecordDto data) {
        if (isCompletedCrossDaySleep(data)) {
            return addCompletedSleepSegments(data);
        }
        return addRecord("sleep", data, SleepRecordDto::getStartTime);
    }

    /**
     * 添加体温记录
     * Add a temperature record
     *
     * @param data 体温记录数据 / temperature record data
     * @return 创建后的体温记录 DTO / created temperature record DTO
     */
    @Override
    public TemperatureRecordDto addTemperatureRecord(TemperatureRecordDto data) {
        return addRecord("temperature", data, TemperatureRecordDto::getTime);
    }

    /**
     * 添加营养品/用药记录，并将自定义补充剂名称保存到用户个人词库
     * Add a supplement/medicine record; save custom supplement name to user's personal dictionary
     *
     * @param data 补充剂记录数据 / supplement record data
     * @return 创建后的补充剂记录 DTO / created supplement record DTO
     */
    @Override
    public SupplementRecordDto addSupplementRecord(SupplementRecordDto data) {
        saveCustomSupplementItemIfNeeded(data);
        return addRecord("supplement", data, SupplementRecordDto::getTime);
    }

    /**
     * 获取当前用户的补充剂选项（包含营养品和药品）
     * Get supplement options for the current user (includes nutrients and medicines)
     * <p>
     * 从用户自定义补充剂记录表中按类型分组返回
     * Loaded from user's custom supplement item table, grouped by type
     *
     * @return 补充剂选项响应（营养品列表 + 药品列表）/ supplement options response
     */
    @Override
    public SupplementOptionsResponse getSupplementOptions() {
        Long userId = currentUserId();
        List<UserSupplementItem> items = userSupplementItemMapper.selectList(new LambdaQueryWrapper<UserSupplementItem>()
                .eq(UserSupplementItem::getUserId, userId)
                .in(UserSupplementItem::getItemType, List.of("supplement", "medicine", DOSE_UNIT_ITEM_TYPE))
                .orderByDesc(UserSupplementItem::getUpdatedAt)
                .orderByDesc(UserSupplementItem::getId));

        SupplementOptionsResponse response = new SupplementOptionsResponse();
        items.forEach(item -> {
            if ("medicine".equals(item.getItemType())) {
                response.getMedicines().add(item.getName());
            } else if (DOSE_UNIT_ITEM_TYPE.equals(item.getItemType())) {
                response.getDoseUnits().add(item.getName());
            } else {
                response.getSupplements().add(item.getName());
            }
        });
        return response;
    }

    /**
     * 添加生长记录（身高体重）
     * Add a growth record (height and weight)
     *
     * @param data 生长记录数据 / growth record data
     * @return 创建后的生长记录 DTO / created growth record DTO
     */
    @Override
    public GrowthRecordDto addGrowthRecord(GrowthRecordDto data) {
        return addRecord("growth", data, GrowthRecordDto::getTime);
    }

    /**
     * 添加异常症状记录（发烧、腹泻、吕吐等）
     * Add an abnormal symptom record (fever, diarrhea, vomit, etc.)
     *
     * @param data 异常记录数据 / abnormal record data
     * @return 创建后的异常记录 DTO / created abnormal record DTO
     */
    @Override
    public AbnormalRecordDto addAbnormalRecord(AbnormalRecordDto data) {
        return addRecord("abnormal", data, AbnormalRecordDto::getTime);
    }

    /**
     * 添加吸奶记录
     * Add a breast pump record
     *
     * @param data 吸奶记录数据 / pump record data
     * @return 创建后的吸奶记录 DTO / created pump record DTO
     */
    @Override
    public PumpRecordDto addPumpRecord(PumpRecordDto data) {
        return addRecord("pump", data, PumpRecordDto::getTime);
    }

    /**
     * 添加辅食记录，并将自定义辅食类型保存到用户词库
     * Add a complementary food record; save custom food types to user's dictionary
     *
     * @param data 辅食记录数据 / complementary food record data
     * @return 创建后的辅食记录 DTO / created complementary record DTO
     */
    @Override
    public ComplementaryRecordDto addComplementaryRecord(ComplementaryRecordDto data) {
        if (data.getCustomFoodTypes() != null) {
            for (String name : data.getCustomFoodTypes()) {
                saveCustomItemIfNeeded(true, name, "complementary");
            }
        }
        return addRecord("complementary", data, ComplementaryRecordDto::getTime);
    }

    @Override
    public MaternalFoodRecordDto addMaternalFoodRecord(MaternalFoodRecordDto data) {
        saveMaternalFoodItemsIfNeeded(data);
        return addRecord("maternal_food", data, MaternalFoodRecordDto::getTime);
    }

    /**
     * 标记当前宝宝今日发烧已恢复，插入一条类型为 fever_resolved 的记录
     * Mark today's fever as resolved by inserting a fever_resolved type record
     */
    @Override
    public void markFeverResolved() {
        insertSimpleRecord("fever_resolved");
    }

    /**
     * 标记当前宝宝今日腹泻已恢复，插入一条类型为 diarrhea_resolved 的记录
     * Mark today's diarrhea as resolved by inserting a diarrhea_resolved type record
     */
    @Override
    public void markDiarrheaResolved() {
        insertSimpleRecord("diarrhea_resolved");
    }

    /**
     * 标记当前宝宝其他异常已恢复，插入一条类型为 abnormal_resolved 的记录
     * Mark other symptoms as resolved by inserting an abnormal_resolved record
     */
    @Override
    public void markAbnormalResolved() {
        insertSimpleRecord("abnormal_resolved");
    }

    /**
     * 添加疫苗接种记录
     * Add a vaccine record
     *
     * @param data 疫苗记录数据 / vaccine record data
     * @return 创建后的疫苗记录 DTO / created vaccine record DTO
     */
    @Override
    public VaccineRecordDto addVaccineRecord(VaccineRecordDto data) {
        validateCustomVaccineRecord(data);
        return addRecord("vaccine", data, VaccineRecordDto::getTime);
    }

    /**
     * 获取当前宝宝的全部疫苗接种记录，按日期倒序返回
     * Get all vaccine records for the current baby, ordered by date DESC
     *
     * @return 疫苗记录 DTO 列表 / list of vaccine record DTOs
     */
    @Override
    public List<VaccineRecordDto> getVaccines() {
        Long userId = currentUserId();
        Long babyId = currentBabyId();
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "vaccine")
                .orderByDesc(ChildRecord::getRecordDate)
                .orderByDesc(ChildRecord::getRecordTime));
        return records.stream()
                .map(record -> Map.entry(record, readPayloadWithId(record, VaccineRecordDto.class)))
                .filter(entry -> isVaccineRecordVisibleForUser(entry.getKey(), entry.getValue(), userId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomVaccineDto> getCustomVaccines() {
        Long userId = currentUserId();
        Long babyId = currentBabyId();
        return userCustomVaccineMapper.selectList(new LambdaQueryWrapper<UserCustomVaccine>()
                        .eq(UserCustomVaccine::getUserId, userId)
                        .eq(UserCustomVaccine::getBabyId, babyId)
                        .orderByAsc(UserCustomVaccine::getDueMonths)
                        .orderByAsc(UserCustomVaccine::getDueWeeks)
                        .orderByAsc(UserCustomVaccine::getDueDays)
                        .orderByAsc(UserCustomVaccine::getId))
                .stream()
                .map(this::toCustomVaccineDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomVaccineDto addCustomVaccine(CustomVaccineDto data) {
        Long userId = currentUserId();
        Long babyId = currentBabyId();
        CustomVaccineDto normalized = normalizeCustomVaccineData(data);
        UserCustomVaccine duplicate = userCustomVaccineMapper.selectOne(new LambdaQueryWrapper<UserCustomVaccine>()
                .eq(UserCustomVaccine::getUserId, userId)
                .eq(UserCustomVaccine::getBabyId, babyId)
                .eq(UserCustomVaccine::getName, normalized.getName())
                .last("limit 1"));
        if (duplicate != null) {
            throw new ResponseStatusException(BAD_REQUEST, "该疫苗已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        UserCustomVaccine entity = new UserCustomVaccine();
        entity.setUserId(userId);
        entity.setBabyId(babyId);
        entity.setName(normalized.getName());
        entity.setCategory(normalized.getCategory());
        entity.setDisease(normalized.getDisease());
        entity.setDoseLabel(normalized.getDoseLabel());
        entity.setAgeLabel(normalized.getAgeLabel());
        Map<String, Integer> due = normalized.getDue();
        entity.setDueDays(due.get("days"));
        entity.setDueWeeks(due.get("weeks"));
        entity.setDueMonths(due.get("months"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userCustomVaccineMapper.insert(entity);
        return toCustomVaccineDto(entity);
    }

    /**
     * 添加活动记录，并将自定义活动名称保存到用户词库
     * Add an activity record; save custom activity name to user's dictionary
     *
     * @param data 活动记录数据 / activity record data
     * @return 创建后的活动记录 DTO / created activity record DTO
     */
    @Override
    public ActivityRecordDto addActivityRecord(ActivityRecordDto data) {
        saveCustomItemIfNeeded(data.getCustomName(), data.getName(), "activity");
        return addRecord("activity", data, ActivityRecordDto::getTime);
    }

    /**
     * 获取当前宝宝的全部活动记录，按日期倒序返回
     * Get all activity records for the current baby, ordered by date DESC
     *
     * @return 活动记录 DTO 列表 / list of activity record DTOs
     */
    @Override
    public List<ActivityRecordDto> getActivities() {
        Long babyId = currentBabyId();
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "activity")
                .orderByDesc(ChildRecord::getRecordDate)
                .orderByDesc(ChildRecord::getRecordTime));
        return records.stream()
                .map(r -> readPayloadWithId(r, ActivityRecordDto.class))
                .collect(Collectors.toList());
    }

    /**
     * 更新已有记录的 payload 并重新同步字段摘要
     * Update an existing record's payload and re-synchronize field summaries
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 根据 ID 查找记录，验证归属于当前宝宝
     *    Find record by ID, verify it belongs to current baby
     * 2. 更新 payloadJson
     *    Update payloadJson
     * 3. 如果是补充剂记录，保存自定义补充剂名称
     *    If supplement record, save custom supplement name
     * 4. 同步记录时间和摘要字段，持久化
     *    Sync record time and summary fields, persist
     *
     * @param id          记录ID / record ID
     * @param payloadJson 新的 payload JSON / new payload JSON
     */
    @Override
    @Transactional
    public void updateRecord(Long id, String payloadJson) {
        ChildRecord record = childRecordMapper.selectById(id);
        if (record == null
                || !Objects.equals(record.getBabyId(), currentBabyId())) {
            throw new ResponseStatusException(NOT_FOUND, "Record not found: " + id);
        }
        Object payload = readPayloadJson(record.getRecordType(), payloadJson);
        if ("sleep".equals(record.getRecordType()) && payload instanceof SleepRecordDto) {
            updateSleepRecord(record, (SleepRecordDto) payload);
            return;
        }
        record.setPayloadJson(payloadJson);
        if (payload instanceof SupplementRecordDto) {
            saveCustomSupplementItemIfNeeded((SupplementRecordDto) payload);
            record.setPayloadJson(writeJson(payload));
        }
        if (payload instanceof MaternalFoodRecordDto) {
            saveMaternalFoodItemsIfNeeded((MaternalFoodRecordDto) payload);
            record.setPayloadJson(writeJson(payload));
        }
        syncRecordTime(record, payload);
        fillRecordSummary(record, payload);
        record.setUpdatedAt(LocalDateTime.now());
        childRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void deleteRecord(Long id) {
        Long babyId = currentBabyId();
        ChildRecord record = childRecordMapper.selectById(id);
        if (record == null
                || !Objects.equals(record.getBabyId(), babyId)) {
            throw new ResponseStatusException(NOT_FOUND, "Record not found: " + id);
        }
        List<ChildRecord> recordsToDelete = "sleep".equals(record.getRecordType())
                ? findLinkedSleepChain(record)
                : List.of(record);
        Set<LocalDate> affectedDates = recordsToDelete.stream()
                .map(ChildRecord::getRecordDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> ids = recordsToDelete.stream()
                .map(ChildRecord::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int updated = softDeleteRecords(babyId, ids);
        if (updated <= 0) {
            throw new ResponseStatusException(NOT_FOUND, "Record not found: " + id);
        }
        affectedDates.forEach(date -> invalidateAiAnalysesCoveringDate(babyId, date));
    }

    private int softDeleteRecords(Long babyId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return childRecordMapper.update(null, new LambdaUpdateWrapper<ChildRecord>()
                .in(ChildRecord::getId, ids)
                .eq(ChildRecord::getBabyId, babyId)
                .set(ChildRecord::getDeleted, 1)
                .set(ChildRecord::getUpdatedAt, LocalDateTime.now()));
    }

    private void invalidateAiAnalysesCoveringDate(Long babyId, LocalDate date) {
        if (date == null) {
            return;
        }
        aiAnalysisRecordMapper.delete(new LambdaQueryWrapper<AiAnalysisRecord>()
                .eq(AiAnalysisRecord::getBabyId, babyId)
                .le(AiAnalysisRecord::getRangeStartDate, date)
                .ge(AiAnalysisRecord::getRangeEndDate, date));
    }

    /**
     * 添加宝宝里程碑记录
     * Add a milestone record for the current baby
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 构建 ChildRecord 实体，类型为 milestone
     *    Build ChildRecord entity with type "milestone"
     * 2. 同步记录时间（支持仅日期的里程碑）
     *    Sync record time (supports date-only milestone)
     * 3. 插入数据库，回写 ID 和 babyId
     *    Persist to DB; write back ID and babyId
     *
     * @param data 里程碑数据 / milestone data
     * @return 创建后的里程碑 DTO / created milestone DTO
     */
    @Override
    public MilestoneRecordDto addMilestone(MilestoneRecordDto data) {
        LocalDateTime now = LocalDateTime.now();
        ChildRecord record = new ChildRecord();
        assignOwnership(record);
        record.setRecordType("milestone");
        record.setRecordDate(now.toLocalDate());
        record.setRecordTime(now);
        record.setPayloadJson(writeJson(data));
        syncRecordTime(record, data);
        fillRecordSummary(record, data);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        childRecordMapper.insert(record);
        data.setId(record.getId());
        data.setBabyId(record.getBabyId());
        return data;
    }

    /**
     * 获取当前宝宝的所有里程碑，按日期升序返回
     * Get all milestones for the current baby, ordered by date ASC
     *
     * @return 里程碑 DTO 列表 / list of milestone DTOs
     */
    @Override
    public List<MilestoneRecordDto> getMilestones() {
        Long babyId = currentBabyId();
        List<ChildRecord> records = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "milestone")
                .orderByAsc(ChildRecord::getRecordDate)
                .orderByAsc(ChildRecord::getId));
        return records.stream()
                .map(r -> readPayloadWithId(r, MilestoneRecordDto.class))
                .collect(Collectors.toList());
    }

    /**
     * 更新里程碑记录
     * Update a milestone record
     * <p>
     * 验证记录属于当前宝宝，更新 payload、时间字段和摘要字段，回写 id 和 babyId
     * Validates record belongs to current baby; updates payload, time fields and summary; writes back id and babyId
     *
     * @param id   里程碑记录ID / milestone record ID
     * @param data 新的里程碑数据 / new milestone data
     * @return 更新后的里程碑 DTO / updated milestone DTO
     */
    @Override
    public MilestoneRecordDto updateMilestone(Long id, MilestoneRecordDto data) {
        ChildRecord record = childRecordMapper.selectById(id);
        if (record == null
                || !Objects.equals(record.getBabyId(), currentBabyId())) {
            throw new ResponseStatusException(NOT_FOUND, "Milestone not found: " + id);
        }
        record.setPayloadJson(writeJson(data));
        syncRecordTime(record, data);
        fillRecordSummary(record, data);
        record.setUpdatedAt(LocalDateTime.now());
        childRecordMapper.updateById(record);
        data.setId(id);
        data.setBabyId(record.getBabyId());
        return data;
    }

    /**
     * 插入一条类型简单、payload 为空对象的恢复记录
     * Insert a simple record with empty payload, used for resolved-status marks
     */
    private void insertSimpleRecord(String recordType) {
        LocalDateTime now = LocalDateTime.now();
        ChildRecord record = new ChildRecord();
        assignOwnership(record);
        record.setRecordType(recordType);
        record.setRecordDate(now.toLocalDate());
        record.setRecordTime(now);
        record.setPayloadJson("{}");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        childRecordMapper.insert(record);
    }

    private CustomVaccineDto normalizeCustomVaccineData(CustomVaccineDto data) {
        if (data == null) {
            throw new ResponseStatusException(BAD_REQUEST, "自定义疫苗不能为空");
        }
        String name = data.getName() == null ? "" : data.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入疫苗名称");
        }
        Map<String, Integer> due = normalizeCustomVaccineDue(data.getDue());
        CustomVaccineDto normalized = new CustomVaccineDto();
        normalized.setName(name);
        normalized.setCategory("free".equals(data.getCategory()) ? "free" : "paid");
        normalized.setDisease(StringUtils.hasText(data.getDisease()) ? data.getDisease().trim() : "自定义疫苗");
        normalized.setDoseLabel(StringUtils.hasText(data.getDoseLabel()) ? data.getDoseLabel().trim() : "1剂");
        normalized.setAgeLabel(StringUtils.hasText(data.getAgeLabel()) ? data.getAgeLabel().trim() : "按门诊安排");
        normalized.setDue(due);
        normalized.setCustom(true);
        return normalized;
    }

    private Map<String, Integer> normalizeCustomVaccineDue(Map<String, Integer> due) {
        if (due == null || due.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入接种时间");
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        copyDueValue(due, normalized, "days");
        copyDueValue(due, normalized, "weeks");
        copyDueValue(due, normalized, "months");
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入接种时间");
        }
        return normalized;
    }

    private void copyDueValue(Map<String, Integer> source, Map<String, Integer> target, String key) {
        Integer value = source.get(key);
        if (value == null) {
            return;
        }
        if (value < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "接种时间不能小于0");
        }
        target.put(key, value);
    }

    private CustomVaccineDto toCustomVaccineDto(UserCustomVaccine entity) {
        CustomVaccineDto dto = new CustomVaccineDto();
        dto.setId(entity.getId());
        dto.setBabyId(entity.getBabyId());
        dto.setName(entity.getName());
        dto.setCategory("free".equals(entity.getCategory()) ? "free" : "paid");
        dto.setDisease(entity.getDisease());
        dto.setDoseLabel(entity.getDoseLabel());
        dto.setAgeLabel(entity.getAgeLabel());
        Map<String, Integer> due = new LinkedHashMap<>();
        if (entity.getDueDays() != null) {
            due.put("days", entity.getDueDays());
        }
        if (entity.getDueWeeks() != null) {
            due.put("weeks", entity.getDueWeeks());
        }
        if (entity.getDueMonths() != null) {
            due.put("months", entity.getDueMonths());
        }
        dto.setDue(due);
        dto.setCustom(true);
        return dto;
    }

    private void validateCustomVaccineRecord(VaccineRecordDto data) {
        boolean customById = data != null && StringUtils.hasText(data.getVaccineId())
                && data.getVaccineId().trim().startsWith("custom_");
        if (!Boolean.TRUE.equals(data == null ? null : data.getCustom()) && !customById) {
            return;
        }
        data.setCustom(true);
        Long customVaccineId = parseCustomVaccineId(data.getVaccineId());
        if (customVaccineId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "自定义疫苗不存在");
        }
        Long userId = currentUserId();
        Long babyId = currentBabyId();
        UserCustomVaccine vaccine = userCustomVaccineMapper.selectOne(new LambdaQueryWrapper<UserCustomVaccine>()
                .eq(UserCustomVaccine::getId, customVaccineId)
                .eq(UserCustomVaccine::getUserId, userId)
                .eq(UserCustomVaccine::getBabyId, babyId)
                .last("limit 1"));
        if (vaccine == null) {
            throw new ResponseStatusException(BAD_REQUEST, "自定义疫苗不存在");
        }
    }

    private Long parseCustomVaccineId(String vaccineId) {
        if (!StringUtils.hasText(vaccineId)) {
            return null;
        }
        String raw = vaccineId.trim();
        if (raw.startsWith("custom_")) {
            raw = raw.substring("custom_".length());
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isVaccineRecordVisibleForUser(ChildRecord record, VaccineRecordDto vaccine, Long userId) {
        boolean customById = vaccine != null && StringUtils.hasText(vaccine.getVaccineId())
                && vaccine.getVaccineId().trim().startsWith("custom_");
        if (!Boolean.TRUE.equals(vaccine == null ? null : vaccine.getCustom()) && !customById) {
            return true;
        }
        return Objects.equals(record.getUserId(), userId);
    }

    /**
     * 获取当前用户自定义的项目列表（补充剂/药品/活动/辅食）
     * Get custom item list for the current user by type (supplement / medicine / activity / complementary)
     *
     * @param type 项目类型 / item type
     * @return 自定义项目响应 / custom items response
     */
    @Override
    public CustomItemsResponse getCustomItems(String type) {
        Long userId = currentUserId();
        String itemType = normalizeItemType(type);
        List<UserSupplementItem> items = userSupplementItemMapper.selectList(new LambdaQueryWrapper<UserSupplementItem>()
                .eq(UserSupplementItem::getUserId, userId)
                .eq(UserSupplementItem::getItemType, itemType)
                .orderByDesc(UserSupplementItem::getUpdatedAt)
                .orderByDesc(UserSupplementItem::getId));
        CustomItemsResponse response = new CustomItemsResponse();
        items.forEach(item -> response.getItems().add(item.getName()));
        return response;
    }

    /**
     * 删除当前用户的自定义项目，不影响已保存的历史记录
     * Delete a custom item for the current user without changing saved records
     */
    @Override
    public void deleteCustomItem(String type, String name) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        Long userId = currentUserId();
        String itemType = normalizeItemType(type);
        String trimmedName = name.trim();
        userSupplementItemMapper.delete(new LambdaQueryWrapper<UserSupplementItem>()
                .eq(UserSupplementItem::getUserId, userId)
                .eq(UserSupplementItem::getItemType, itemType)
                .eq(UserSupplementItem::getName, trimmedName));
    }

    /**
     * 如果 custom 标志为 true 且名称非空，将自定义项目名存入用户词库（已存则更新时间，不存则新建）
     * Save custom item name to user's dictionary if custom flag is true and name is non-empty
     * (update updatedAt if exists; create new entry otherwise)
     */
    private void saveCustomItemIfNeeded(Boolean custom, String name, String type) {
        if (!Boolean.TRUE.equals(custom) || !StringUtils.hasText(name)) {
            return;
        }
        Long userId = currentUserId();
        String itemType = normalizeItemType(type);
        String trimmedName = name.trim();
        UserSupplementItem existing = userSupplementItemMapper.selectOne(new LambdaQueryWrapper<UserSupplementItem>()
                .eq(UserSupplementItem::getUserId, userId)
                .eq(UserSupplementItem::getItemType, itemType)
                .eq(UserSupplementItem::getName, trimmedName)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setUpdatedAt(now);
            userSupplementItemMapper.updateById(existing);
            return;
        }
        UserSupplementItem item = new UserSupplementItem();
        item.setUserId(userId);
        item.setItemType(itemType);
        item.setName(trimmedName);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        userSupplementItemMapper.insert(item);
    }

    /**
     * 将项目类型字符串规范化为数据库存儲的标准类型：medicine / activity / complementary / supplement
     * Normalize item type string to standard DB storage type: medicine / activity / complementary / supplement
     */
    private void saveMaternalFoodItemsIfNeeded(MaternalFoodRecordDto data) {
        if (data == null || data.getCustomFoods() == null) {
            return;
        }
        for (String name : data.getCustomFoods()) {
            saveCustomItemIfNeeded(true, name, "maternal_food");
        }
    }

    private String normalizeItemType(String type) {
        if (type == null) return "supplement";
        switch (type) {
            case "medicine": return "medicine";
            case "activity": return "activity";
            case "complementary": return "complementary";
            case "maternalFood":
            case "maternal_food": return "maternal_food";
            case "doseUnit":
            case "dose_unit": return DOSE_UNIT_ITEM_TYPE;
            default: return "supplement";
        }
    }

    /**
     * 如果补充剂记录中有自定义名称，将其存入用户词库，并规范化 type 和 name 字段
     * If supplement record contains a custom name, save to user's dictionary;
     * also normalizes the type and name fields in place
     */
    private void saveCustomSupplementItemIfNeeded(SupplementRecordDto data) {
        if (data == null) {
            return;
        }
        data.setType(normalizeSupplementType(data.getType()));
        if (StringUtils.hasText(data.getName())) {
            data.setName(data.getName().trim());
        }
        if (StringUtils.hasText(data.getDoseUnit())) {
            data.setDoseUnit(data.getDoseUnit().trim());
        }
        saveCustomDoseUnitIfNeeded(data);
        if (!Boolean.TRUE.equals(data.getCustomName()) || !StringUtils.hasText(data.getName())) {
            return;
        }

        Long userId = currentUserId();
        UserSupplementItem existing = userSupplementItemMapper.selectOne(new LambdaQueryWrapper<UserSupplementItem>()
                .eq(UserSupplementItem::getUserId, userId)
                .eq(UserSupplementItem::getItemType, data.getType())
                .eq(UserSupplementItem::getName, data.getName())
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setUpdatedAt(now);
            userSupplementItemMapper.updateById(existing);
            return;
        }

        UserSupplementItem item = new UserSupplementItem();
        item.setUserId(userId);
        item.setItemType(data.getType());
        item.setName(data.getName());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        userSupplementItemMapper.insert(item);
    }

    private void saveCustomDoseUnitIfNeeded(SupplementRecordDto data) {
        if (data == null || !Boolean.TRUE.equals(data.getCustomDoseUnit()) || !StringUtils.hasText(data.getDoseUnit())) {
            return;
        }
        String doseUnit = data.getDoseUnit().trim();
        if (DEFAULT_DOSE_UNITS.contains(doseUnit)) {
            return;
        }
        saveCustomItemIfNeeded(true, doseUnit, DOSE_UNIT_ITEM_TYPE);
    }

    /**
     * 规范化补充剂类型：medicine 保持不变，其他一律转为 supplement
     * Normalize supplement type: keep "medicine" as is; convert all others to "supplement"
     */
    private String normalizeSupplementType(String type) {
        return "medicine".equals(type) ? "medicine" : "supplement";
    }

    /**
     * 获取当前宝宝今日最新的睡眠记录
     * Get the latest sleep record for the current baby today
     *
     * @return 睡眠记录 DTO，没有则返回 null / sleep record DTO, null if none
     */
    @Override
    @Transactional
    public SleepRecordDto getLatestSleep() {
        Long babyId = currentBabyId();
        ChildRecord active = splitOpenSleepIfNeeded(babyId, LocalDateTime.now());
        splitCompletedCrossDaySleepsIfNeeded(babyId);
        if (active != null) {
            return readSleepPayloadForDisplay(active);
        }
        ChildRecord record = childRecordMapper.selectOne(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "sleep")
                .eq(ChildRecord::getRecordDate, LocalDate.now())
                .orderByDesc(ChildRecord::getRecordTime)
                .orderByDesc(ChildRecord::getId)
                .last("limit 1"));
        if (record == null) return null;
        return readSleepPayloadForDisplay(record);
    }

    /**
     * 将指定睡眠记录标记为已结束（却寭），自动计算持续时长
     * Mark the specified sleep record as ended (wake up); auto-calculate duration
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 根据 sleepId 查找记录，验证类型为 sleep 且属于当前宝宝
     *    Find record by sleepId, verify type is "sleep" and belongs to current baby
     * 2. 如果记录尚未设置结束时间，将当前时间设为结束时间
     *    If endTime not set, set current time as end time
     * 3. 基于开始时间和当前时间计算时长（分钟，最小 1 分钟）
     *    Calculate duration in minutes from start to now (min 1 minute)
     * 4. 更新 payload 和摘要字段并持久化
     *    Update payload and summary fields, persist
     * 5. 返回更新后的睡眠记录 DTO
     *    Return updated sleep record DTO
     *
     * @param sleepId 睡眠记录ID / sleep record ID
     * @return 更新后的睡眠记录 DTO / updated sleep record DTO
     */
    @Override
    @Transactional
    public SleepRecordDto wakeUpSleep(Long sleepId) {
        ChildRecord record = childRecordMapper.selectOne(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getId, sleepId)
                .eq(ChildRecord::getBabyId, currentBabyId())
                .last("limit 1"));
        if (record == null || !"sleep".equals(record.getRecordType())) {
            throw new ResponseStatusException(NOT_FOUND, "Sleep record not found: " + sleepId);
        }

        SleepRecordDto sleep = readPayload(record, SleepRecordDto.class);
        sleep.setId(record.getId());
        sleep.setBabyId(record.getBabyId());
        if (!StringUtils.hasText(sleep.getEndTime())) {
            LocalDateTime now = LocalDateTime.now();
            record = splitOpenSleepIfNeeded(record, now);
            sleep = readPayload(record, SleepRecordDto.class);
            sleep.setId(record.getId());
            sleep.setBabyId(record.getBabyId());
            LocalDateTime start = sleepStartTime(record, sleep);
            long duration = Math.max(1L, (Duration.between(start, now).toMillis() + 59999L) / 60000L);
            sleep.setEndTime(FRONT_TIME.format(now));
            sleep.setDuration(Math.toIntExact(duration));
            record.setPayloadJson(writeJson(sleep));
            fillRecordSummary(record, sleep);
            record.setUpdatedAt(now);
            childRecordMapper.updateById(record);
        }
        return sleep;
    }

    /**
     * Split an unfinished sleep record at midnight so every day owns only its own sleep duration.
     */
    private ChildRecord splitOpenSleepIfNeeded(Long babyId, LocalDateTime now) {
        ChildRecord record = findLatestOpenSleepRecord(babyId);
        if (record == null) {
            return null;
        }
        return splitOpenSleepIfNeeded(record, now);
    }

    private ChildRecord splitOpenSleepIfNeeded(ChildRecord record, LocalDateTime now) {
        SleepRecordDto sleep = readPayload(record, SleepRecordDto.class);
        if (StringUtils.hasText(sleep.getEndTime())) {
            return record;
        }

        LocalDateTime start = sleepStartTime(record, sleep);
        if (!start.toLocalDate().isBefore(now.toLocalDate())) {
            return record;
        }

        ChildRecord currentRecord = record;
        SleepRecordDto currentSleep = sleep;
        LocalDateTime currentStart = start;
        while (currentStart.toLocalDate().isBefore(now.toLocalDate())) {
            LocalDateTime boundary = currentStart.toLocalDate().plusDays(1).atStartOfDay();
            finishSleepRecord(currentRecord, currentSleep, currentStart, boundary, now);

            SleepRecordDto nextSleep = new SleepRecordDto();
            nextSleep.setStartTime(FRONT_TIME.format(boundary));
            nextSleep.setNote(currentSleep.getNote());
            currentRecord = insertSleepRecordSegment(nextSleep, currentRecord.getUserId(), currentRecord.getBabyId(), boundary, now);
            currentSleep = nextSleep;
            currentStart = boundary;
        }
        return currentRecord;
    }

    private ChildRecord findLatestOpenSleepRecord(Long babyId) {
        List<ChildRecord> candidates = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "sleep")
                .isNull(ChildRecord::getDurationSec)
                .orderByDesc(ChildRecord::getRecordTime)
                .orderByDesc(ChildRecord::getId)
                .last("limit 20"));
        for (ChildRecord candidate : candidates) {
            SleepRecordDto sleep = readPayload(candidate, SleepRecordDto.class);
            if (!StringUtils.hasText(sleep.getEndTime())) {
                return candidate;
            }
        }
        return null;
    }

    private void splitCompletedCrossDaySleepsIfNeeded(Long babyId) {
        List<ChildRecord> candidates = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .eq(ChildRecord::getRecordType, "sleep")
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));
        for (ChildRecord candidate : candidates) {
            SleepRecordDto sleep = readPayload(candidate, SleepRecordDto.class);
            if (isCompletedCrossDaySleep(sleep)) {
                updateCompletedSleepSegments(candidate, sleep);
            }
        }
    }

    private boolean isCompletedCrossDaySleep(SleepRecordDto sleep) {
        if (sleep == null || !StringUtils.hasText(sleep.getStartTime()) || !StringUtils.hasText(sleep.getEndTime())) {
            return false;
        }
        LocalDateTime start = parseFrontTime(sleep.getStartTime());
        LocalDateTime end = parseFrontTime(sleep.getEndTime());
        LocalDateTime firstMidnight = start.toLocalDate().plusDays(1).atStartOfDay();
        return end.isAfter(firstMidnight);
    }

    private SleepRecordDto addCompletedSleepSegments(SleepRecordDto data) {
        LocalDateTime start = parseFrontTime(data.getStartTime());
        LocalDateTime end = parseFrontTime(data.getEndTime());
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        Long babyId = currentBabyId();
        SleepRecordDto last = null;

        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalDateTime boundary = cursor.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = boundary.isBefore(end) ? boundary : end;

            SleepRecordDto segment = new SleepRecordDto();
            segment.setStartTime(FRONT_TIME.format(cursor));
            segment.setEndTime(FRONT_TIME.format(segmentEnd));
            segment.setDuration(durationMinutes(cursor, segmentEnd));
            segment.setNote(data.getNote());
            ChildRecord record = insertSleepRecordSegment(segment, userId, babyId, cursor, now);
            last = readPayloadWithId(record, SleepRecordDto.class);
            cursor = segmentEnd;
        }
        return last == null ? addRecord("sleep", data, SleepRecordDto::getStartTime) : last;
    }

    private void updateSleepRecord(ChildRecord record, SleepRecordDto data) {
        SleepRecordDto original = readPayload(record, SleepRecordDto.class);
        List<ChildRecord> chain = findLinkedSleepChain(record);
        if (chain.size() > 1 || isCompletedCrossDaySleep(data)) {
            updateCompletedSleepSegments(record, original, data, chain);
            return;
        }

        data.setId(record.getId());
        data.setBabyId(record.getBabyId());
        record.setPayloadJson(writeJson(data));
        syncRecordTime(record, data);
        fillRecordSummary(record, data);
        record.setUpdatedAt(LocalDateTime.now());
        childRecordMapper.updateById(record);
    }

    private void updateCompletedSleepSegments(ChildRecord record, SleepRecordDto data) {
        updateCompletedSleepSegments(record, readPayload(record, SleepRecordDto.class), data, findLinkedSleepChain(record));
    }

    private void updateCompletedSleepSegments(ChildRecord record, SleepRecordDto original,
                                              SleepRecordDto data, List<ChildRecord> chain) {
        SleepRecordDto first = readPayload(chain.get(0), SleepRecordDto.class);
        SleepRecordDto last = readPayload(chain.get(chain.size() - 1), SleepRecordDto.class);

        String requestedStart = StringUtils.hasText(data.getStartTime()) ? data.getStartTime() : original.getStartTime();
        String requestedEnd = StringUtils.hasText(data.getEndTime()) ? data.getEndTime() : original.getEndTime();
        String wholeStart = !Objects.equals(requestedStart, original.getStartTime()) ? requestedStart : first.getStartTime();
        String wholeEnd = !Objects.equals(requestedEnd, original.getEndTime()) ? requestedEnd : last.getEndTime();
        if (!StringUtils.hasText(wholeStart) || !StringUtils.hasText(wholeEnd)) {
            data.setId(record.getId());
            data.setBabyId(record.getBabyId());
            record.setPayloadJson(writeJson(data));
            syncRecordTime(record, data);
            fillRecordSummary(record, data);
            record.setUpdatedAt(LocalDateTime.now());
            childRecordMapper.updateById(record);
            return;
        }

        SleepRecordDto fullSleep = new SleepRecordDto();
        fullSleep.setStartTime(wholeStart);
        fullSleep.setEndTime(wholeEnd);
        fullSleep.setNote(data.getNote());
        rewriteSleepChain(record, chain, fullSleep);
    }

    private List<ChildRecord> findLinkedSleepChain(ChildRecord anchor) {
        List<ChildRecord> all = childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, anchor.getBabyId())
                .eq(ChildRecord::getRecordType, "sleep")
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));
        Map<Long, SleepRecordDto> payloads = new LinkedHashMap<>();
        for (ChildRecord item : all) {
            payloads.put(item.getId(), readPayload(item, SleepRecordDto.class));
        }

        Set<Long> chainIds = new HashSet<>();
        chainIds.add(anchor.getId());
        boolean added;
        do {
            added = false;
            Set<String> chainStarts = new HashSet<>();
            Set<String> chainEnds = new HashSet<>();
            for (ChildRecord item : all) {
                if (!chainIds.contains(item.getId())) {
                    continue;
                }
                SleepRecordDto sleep = payloads.get(item.getId());
                if (isMidnightText(sleep.getStartTime())) {
                    chainStarts.add(sleep.getStartTime());
                }
                if (isMidnightText(sleep.getEndTime())) {
                    chainEnds.add(sleep.getEndTime());
                }
            }
            for (ChildRecord item : all) {
                if (chainIds.contains(item.getId())) {
                    continue;
                }
                SleepRecordDto sleep = payloads.get(item.getId());
                boolean previousSegment = StringUtils.hasText(sleep.getEndTime()) && chainStarts.contains(sleep.getEndTime());
                boolean nextSegment = StringUtils.hasText(sleep.getStartTime()) && chainEnds.contains(sleep.getStartTime());
                if (previousSegment || nextSegment) {
                    chainIds.add(item.getId());
                    added = true;
                }
            }
        } while (added);

        return all.stream()
                .filter(item -> chainIds.contains(item.getId()))
                .sorted(Comparator
                        .comparing((ChildRecord item) -> sleepStartSortValue(payloads.get(item.getId()), item))
                        .thenComparing(ChildRecord::getId))
                .collect(Collectors.toList());
    }

    private boolean isMidnightText(String time) {
        if (!StringUtils.hasText(time)) {
            return false;
        }
        return LocalTime.MIDNIGHT.equals(parseFrontTime(time).toLocalTime());
    }

    private LocalDateTime sleepStartSortValue(SleepRecordDto sleep, ChildRecord fallback) {
        if (sleep != null && StringUtils.hasText(sleep.getStartTime())) {
            return parseFrontTime(sleep.getStartTime());
        }
        return fallback.getRecordTime();
    }

    private void rewriteSleepChain(ChildRecord anchor, List<ChildRecord> reusableRecords, SleepRecordDto data) {
        LocalDateTime start = parseFrontTime(data.getStartTime());
        LocalDateTime end = parseFrontTime(data.getEndTime());
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(BAD_REQUEST, "Sleep end time must be after start time");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cursor = start;
        int index = 0;

        while (cursor.isBefore(end)) {
            LocalDateTime boundary = cursor.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = boundary.isBefore(end) ? boundary : end;

            SleepRecordDto segment = new SleepRecordDto();
            segment.setStartTime(FRONT_TIME.format(cursor));
            segment.setEndTime(FRONT_TIME.format(segmentEnd));
            segment.setDuration(durationMinutes(cursor, segmentEnd));
            segment.setNote(data.getNote());

            if (index < reusableRecords.size()) {
                applySleepSegmentToRecord(reusableRecords.get(index), segment, cursor, now);
            } else {
                insertSleepRecordSegment(segment, anchor.getUserId(), anchor.getBabyId(), cursor, now);
            }
            cursor = segmentEnd;
            index++;
        }

        for (int i = index; i < reusableRecords.size(); i++) {
            childRecordMapper.deleteById(reusableRecords.get(i).getId());
        }
    }

    private void applySleepSegmentToRecord(ChildRecord record, SleepRecordDto segment,
                                           LocalDateTime recordTime, LocalDateTime updatedAt) {
        segment.setId(record.getId());
        segment.setBabyId(record.getBabyId());
        record.setRecordDate(recordTime.toLocalDate());
        record.setRecordTime(recordTime);
        record.setPayloadJson(writeJson(segment));
        fillRecordSummary(record, segment);
        record.setUpdatedAt(updatedAt);
        childRecordMapper.updateById(record);
    }

    private void finishSleepRecord(ChildRecord record, SleepRecordDto sleep,
                                   LocalDateTime start, LocalDateTime end, LocalDateTime updatedAt) {
        sleep.setId(record.getId());
        sleep.setBabyId(record.getBabyId());
        sleep.setStartTime(FRONT_TIME.format(start));
        sleep.setEndTime(FRONT_TIME.format(end));
        sleep.setDuration(durationMinutes(start, end));
        record.setPayloadJson(writeJson(sleep));
        fillRecordSummary(record, sleep);
        record.setUpdatedAt(updatedAt);
        childRecordMapper.updateById(record);
    }

    private ChildRecord insertSleepRecordSegment(SleepRecordDto sleep, Long userId, Long babyId,
                                                 LocalDateTime recordTime, LocalDateTime now) {
        ChildRecord record = new ChildRecord();
        record.setUserId(userId);
        record.setBabyId(babyId);
        record.setRecordType("sleep");
        record.setRecordDate(recordTime.toLocalDate());
        record.setRecordTime(recordTime);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setPayloadJson(writeJson(sleep));
        fillRecordSummary(record, sleep);
        childRecordMapper.insert(record);

        sleep.setId(record.getId());
        sleep.setBabyId(record.getBabyId());
        record.setPayloadJson(writeJson(sleep));
        fillRecordSummary(record, sleep);
        childRecordMapper.updateById(record);
        return record;
    }

    private LocalDateTime sleepStartTime(ChildRecord record, SleepRecordDto sleep) {
        return StringUtils.hasText(sleep.getStartTime())
                ? parseFrontTime(sleep.getStartTime())
                : record.getRecordTime();
    }

    private int durationMinutes(LocalDateTime start, LocalDateTime end) {
        long duration = Math.max(1L, (Duration.between(start, end).toMillis() + 59999L) / 60000L);
        return Math.toIntExact(duration);
    }

    /**
     * 通用记录新增方法：构建 ChildRecord 实体、写入 payload、同步摘要字段并持久化
     * Generic record creation: build ChildRecord entity, write payload, sync summary fields and persist
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 从 data 中提取时间字符串并解析为 LocalDateTime（为空则取当前时间）
     *    Extract time string from data and parse to LocalDateTime (default to now if empty)
     * 2. 构建 ChildRecord，赋值用户/宝宝/类型/日期/时间
     *    Build ChildRecord, assign user/baby/type/date/time
     * 3. 将 data 序列化为 JSON 并填充摘要字段，插入数据库获得 ID
     *    Serialize data to JSON, fill summary fields, insert to DB to get ID
     * 4. 回写 ID 和 babyId到 data，重新序列化并更新记录
     *    Write back ID and babyId to data, re-serialize and update record in DB
     * 5. 返回 data
     *    Return data
     *
     * @param recordType    记录类型 / record type
     * @param data          记录数据 DTO / record data DTO
     * @param timeExtractor 从 DTO 中提取时间字符串的函数 / function to extract time string from DTO
     * @return 创建后的 DTO（含 ID 和 babyId）/ created DTO (with ID and babyId)
     */
    private <T extends BaseRecordDto> T addRecord(String recordType, T data, Function<T, String> timeExtractor) {
        LocalDateTime recordTime = resolveRecordTime(timeExtractor.apply(data));
        LocalDateTime now = LocalDateTime.now();

        ChildRecord record = new ChildRecord();
        assignOwnership(record);
        record.setRecordType(recordType);
        record.setRecordDate(recordTime.toLocalDate());
        record.setRecordTime(recordTime);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setPayloadJson(writeJson(data));
        fillRecordSummary(record, data);
        childRecordMapper.insert(record);

        data.setId(record.getId());
        data.setBabyId(record.getBabyId());
        record.setPayloadJson(writeJson(data));
        fillRecordSummary(record, data);
        record.setUpdatedAt(LocalDateTime.now());
        childRecordMapper.updateById(record);
        return data;
    }

    /**
     * 根据记录类型将 payloadJson 字符串反序列化为对应的 DTO 对象
     * Deserialize payloadJson string to the corresponding DTO object based on record type
     */
    private Object readPayloadJson(String recordType, String payloadJson) {
        try {
            switch (recordType) {
                case "feed":
                    return objectMapper.readValue(payloadJson, FeedRecordDto.class);
                case "sleep":
                    return objectMapper.readValue(payloadJson, SleepRecordDto.class);
                case "diaper":
                    return objectMapper.readValue(payloadJson, DiaperRecordDto.class);
                case "temperature":
                    return objectMapper.readValue(payloadJson, TemperatureRecordDto.class);
                case "supplement":
                    return objectMapper.readValue(payloadJson, SupplementRecordDto.class);
                case "growth":
                    return objectMapper.readValue(payloadJson, GrowthRecordDto.class);
                case "pump":
                    return objectMapper.readValue(payloadJson, PumpRecordDto.class);
                case "complementary":
                    return objectMapper.readValue(payloadJson, ComplementaryRecordDto.class);
                case "maternal_food":
                    return objectMapper.readValue(payloadJson, MaternalFoodRecordDto.class);
                case "abnormal":
                    return objectMapper.readValue(payloadJson, AbnormalRecordDto.class);
                case "vaccine":
                    return objectMapper.readValue(payloadJson, VaccineRecordDto.class);
                case "activity":
                    return objectMapper.readValue(payloadJson, ActivityRecordDto.class);
                case "milestone":
                    return objectMapper.readValue(payloadJson, MilestoneRecordDto.class);
                default:
                    return null;
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid record payload for type: " + recordType, e);
        }
    }

    /**
     * 将 payload 中的时间字段同步到 ChildRecord 的 recordTime 和 recordDate
     * Sync the time field from payload into ChildRecord's recordTime and recordDate
     * <p>
     * MilestoneRecordDto 如果只有日期则使用当天起始时间
     * For MilestoneRecordDto, if only date is provided, use start-of-day
     */
    private void syncRecordTime(ChildRecord record, Object payload) {
        String frontTime = extractFrontTime(payload);
        if (!StringUtils.hasText(frontTime)) {
            return;
        }
        LocalDateTime recordTime;
        if (payload instanceof MilestoneRecordDto && frontTime.length() == 10) {
            recordTime = LocalDate.parse(frontTime).atStartOfDay();
        } else {
            recordTime = parseFrontTime(frontTime);
        }
        record.setRecordTime(recordTime);
        record.setRecordDate(recordTime.toLocalDate());
    }

    /**
     * 从 payload 对象中提取时间字符串，每种 DTO 类型对应不同的字段名
     * Extract time string from a payload object; each DTO type has a different time field name
     */
    private String extractFrontTime(Object payload) {
        if (payload instanceof FeedRecordDto) return ((FeedRecordDto) payload).getTime();
        if (payload instanceof SleepRecordDto) return ((SleepRecordDto) payload).getStartTime();
        if (payload instanceof DiaperRecordDto) return ((DiaperRecordDto) payload).getTime();
        if (payload instanceof TemperatureRecordDto) return ((TemperatureRecordDto) payload).getTime();
        if (payload instanceof SupplementRecordDto) return ((SupplementRecordDto) payload).getTime();
        if (payload instanceof GrowthRecordDto) return ((GrowthRecordDto) payload).getTime();
        if (payload instanceof PumpRecordDto) return ((PumpRecordDto) payload).getTime();
        if (payload instanceof ComplementaryRecordDto) return ((ComplementaryRecordDto) payload).getTime();
        if (payload instanceof MaternalFoodRecordDto) return ((MaternalFoodRecordDto) payload).getTime();
        if (payload instanceof AbnormalRecordDto) return ((AbnormalRecordDto) payload).getTime();
        if (payload instanceof VaccineRecordDto) return ((VaccineRecordDto) payload).getTime();
        if (payload instanceof ActivityRecordDto) return ((ActivityRecordDto) payload).getTime();
        if (payload instanceof MilestoneRecordDto) return ((MilestoneRecordDto) payload).getDate();
        return null;
    }

    /**
     * 根据 payload 类型将关键数据同步到 ChildRecord 的小云字段，便于建索查询
     * Sync key data from payload into ChildRecord's cloud fields for index/query purposes
     * <p>
     * 包含：recordSubType、amountMl、durationSec、左右时长、abnormalFlag、体温、身高、体重
     * Includes: recordSubType, amountMl, durationSec, left/right duration, abnormalFlag, temperature, height, weight
     */
    private void fillRecordSummary(ChildRecord record, Object payload) {
        record.setRecordSubType(null);
        record.setAmountMl(null);
        record.setDurationSec(null);
        record.setLeftDurationSec(null);
        record.setRightDurationSec(null);
        record.setAbnormalFlag(null);
        record.setTemperatureValue(null);
        record.setHeightCm(null);
        record.setWeightKg(null);

        if (payload instanceof FeedRecordDto) {
            FeedRecordDto feed = (FeedRecordDto) payload;
            record.setRecordSubType(feed.getType());
            if ("breast".equals(feed.getType())) {
                int leftSec = durationSeconds(feed.getLeftDurationSec(), feed.getLeftDuration());
                int rightSec = durationSeconds(feed.getRightDurationSec(), feed.getRightDuration());
                int totalSec = leftSec + rightSec;
                if (totalSec == 0 && feed.getDuration() != null) {
                    totalSec = feed.getDuration() * 60;
                }
                record.setLeftDurationSec(leftSec);
                record.setRightDurationSec(rightSec);
                record.setDurationSec(totalSec);
            } else {
                record.setAmountMl(feed.getAmount());
            }
            return;
        }
        if (payload instanceof SleepRecordDto) {
            SleepRecordDto sleep = (SleepRecordDto) payload;
            record.setDurationSec(sleep.getDuration() == null ? null : sleep.getDuration() * 60);
            return;
        }
        if (payload instanceof DiaperRecordDto) {
            DiaperRecordDto diaper = (DiaperRecordDto) payload;
            boolean diarrhea = diaper.getDiarrhea() != null && !diaper.getDiarrhea().isEmpty();
            record.setRecordSubType(diaper.getType());
            record.setAbnormalFlag(Boolean.TRUE.equals(diaper.getAbnormal()) || diarrhea);
            return;
        }
        if (payload instanceof TemperatureRecordDto) {
            TemperatureRecordDto temperature = (TemperatureRecordDto) payload;
            record.setTemperatureValue(temperature.getTemperature());
            record.setAbnormalFlag(Boolean.TRUE.equals(temperature.getIsAbnormal()) || isFever(temperature.getTemperature()));
            return;
        }
        if (payload instanceof SupplementRecordDto) {
            record.setRecordSubType(((SupplementRecordDto) payload).getType());
            return;
        }
        if (payload instanceof GrowthRecordDto) {
            GrowthRecordDto growth = (GrowthRecordDto) payload;
            record.setHeightCm(growth.getHeight());
            record.setWeightKg(growth.getWeight());
            return;
        }
        if (payload instanceof PumpRecordDto) {
            PumpRecordDto pump = (PumpRecordDto) payload;
            record.setAmountMl(pump.getTotalAmount());
            record.setLeftDurationSec(safeInt(pump.getLeftDuration()) * 60);
            record.setRightDurationSec(safeInt(pump.getRightDuration()) * 60);
            record.setDurationSec(record.getLeftDurationSec() + record.getRightDurationSec());
            return;
        }
        if (payload instanceof ComplementaryRecordDto) {
            record.setAbnormalFlag(Boolean.TRUE.equals(((ComplementaryRecordDto) payload).getAbnormal()));
            return;
        }
        if (payload instanceof MaternalFoodRecordDto) {
            record.setRecordSubType(((MaternalFoodRecordDto) payload).getMealType());
            return;
        }
        if (payload instanceof AbnormalRecordDto) {
            AbnormalRecordDto abnormal = (AbnormalRecordDto) payload;
            record.setAbnormalFlag(true);
            record.setTemperatureValue(abnormal.getTemperature());
            if (isFever(abnormal.getTemperature())) {
                record.setRecordSubType("fever");
            } else if (abnormal.getDiarrhea() != null && !abnormal.getDiarrhea().isEmpty()) {
                record.setRecordSubType("diarrhea");
            } else if (StringUtils.hasText(abnormal.getVomit())) {
                record.setRecordSubType("vomit");
            } else if (StringUtils.hasText(abnormal.getOther())) {
                record.setRecordSubType("other");
            } else if (abnormal.getMedicine() != null) {
                record.setRecordSubType("other");
            }
            return;
        }
        if (payload instanceof ActivityRecordDto) {
            ActivityRecordDto activity = (ActivityRecordDto) payload;
            record.setRecordSubType(activity.getCategory());
            record.setDurationSec(safeInt(activity.getDuration()) * 60);
        }
    }

    /**
     * 将单条 ChildRecord 按类型分散到 DailyStatsResponse 的对应统计字段
     * Scatter a single ChildRecord by type into the corresponding stats field of DailyStatsResponse
     */
    private void addToDailyStats(DailyStatsResponse stats, ChildRecord record) {
        if ("fever_resolved".equals(record.getRecordType())
                || "diarrhea_resolved".equals(record.getRecordType())
                || "abnormal_resolved".equals(record.getRecordType())) {
            return;
        }
        stats.setRecordCount(stats.getRecordCount() + 1);
        switch (record.getRecordType()) {
            case "feed":
                addFeedStats(stats.getFeed(), readPayloadWithId(record, FeedRecordDto.class));
                break;
            case "sleep":
                addSleepStats(stats.getSleep(), readPayloadWithId(record, SleepRecordDto.class));
                break;
            case "diaper":
                DiaperRecordDto diaper = readPayloadWithId(record, DiaperRecordDto.class);
                addDiaperStats(stats.getDiaper(), diaper);
                addDiaperAbnormalStats(stats.getAbnormal(), diaper);
                break;
            case "temperature":
                TemperatureRecordDto temperature = readPayloadWithId(record, TemperatureRecordDto.class);
                addTemperatureStats(stats.getTemperature(), temperature);
                addTemperatureAbnormalStats(stats.getAbnormal(), temperature);
                break;
            case "supplement":
                addSupplementStats(stats.getSupplement(), readPayloadWithId(record, SupplementRecordDto.class));
                break;
            case "growth":
                addGrowthStats(stats.getGrowth(), readPayloadWithId(record, GrowthRecordDto.class));
                break;
            case "pump":
                addPumpStats(stats.getPump(), readPayloadWithId(record, PumpRecordDto.class));
                break;
            case "complementary":
                addComplementaryStats(stats.getComplementary(), readPayloadWithId(record, ComplementaryRecordDto.class));
                break;
            case "maternal_food":
                addMaternalFoodStats(stats.getMaternalFood(), readPayloadWithId(record, MaternalFoodRecordDto.class));
                break;
            case "abnormal":
                addAbnormalStats(stats.getAbnormal(), readPayloadWithId(record, AbnormalRecordDto.class));
                break;
            case "vaccine":
                VaccineRecordDto vaccine = readPayloadWithId(record, VaccineRecordDto.class);
                if (!Boolean.TRUE.equals(vaccine.getSkipped()) && !"skipped".equals(vaccine.getStatus())) {
                    stats.getVaccine().setCount(stats.getVaccine().getCount() + 1);
                }
                break;
            case "activity":
                addActivityStats(stats.getActivity(), readPayloadWithId(record, ActivityRecordDto.class));
                break;
            case "milestone":
                stats.getMilestone().setCount(stats.getMilestone().getCount() + 1);
                break;
            default:
                break;
        }
    }

    /**
     * 将喂养记录叠加到喂养统计（小数量、母乳量、胸奶时长等）
     * Accumulate feed record into FeedStats (bottle count/amount, expressed amount, breast duration, etc.)
     */
    private void addFeedStats(DailyStatsResponse.FeedStats stats, FeedRecordDto feed) {
        stats.setCount(stats.getCount() + 1);
        if (StringUtils.hasText(feed.getTime())
                && (stats.getLastFeedTime() == null || feed.getTime().compareTo(stats.getLastFeedTime()) > 0)) {
            stats.setLastFeedTime(feed.getTime());
        }
        String type = Optional.ofNullable(feed.getType()).orElse("");
        if ("breast".equals(type)) {
            int leftSec = durationSeconds(feed.getLeftDurationSec(), feed.getLeftDuration());
            int rightSec = durationSeconds(feed.getRightDurationSec(), feed.getRightDuration());
            if (StringUtils.hasText(feed.getLeftStartTime())) {
                leftSec += elapsedSeconds(feed.getLeftStartTime());
            }
            if (StringUtils.hasText(feed.getRightStartTime())) {
                rightSec += elapsedSeconds(feed.getRightStartTime());
            }
            int totalSec = leftSec + rightSec;
            if (totalSec == 0 && feed.getDuration() != null) {
                totalSec = feed.getDuration() * 60;
            }
            stats.setBreastCount(stats.getBreastCount() + 1);
            stats.setBreastLeftDurationSec(stats.getBreastLeftDurationSec() + leftSec);
            stats.setBreastRightDurationSec(stats.getBreastRightDurationSec() + rightSec);
            stats.setBreastDurationSec(stats.getBreastDurationSec() + totalSec);
        } else if ("expressed".equals(type)) {
            int amount = safeInt(feed.getAmount());
            stats.setExpressedCount(stats.getExpressedCount() + 1);
            stats.setExpressedMilk(stats.getExpressedMilk() + amount);
            stats.setTotalMilk(stats.getTotalMilk() + amount);
        } else {
            int amount = safeInt(feed.getAmount());
            stats.setBottleCount(stats.getBottleCount() + 1);
            stats.setBottleMilk(stats.getBottleMilk() + amount);
            stats.setTotalMilk(stats.getTotalMilk() + amount);
        }
    }

    /**
     * 将睡眠记录叠加到睡眠统计（次数、总时长、进行中睡眠数）
     * Accumulate sleep record into SleepStats (count, total duration, ongoing count)
     */
    private void addSleepStats(DailyStatsResponse.SleepStats stats, SleepRecordDto sleep) {
        stats.setCount(stats.getCount() + 1);
        int durationSec = sleep.getDuration() == null ? 0 : sleep.getDuration() * 60;
        if (durationSec == 0 && StringUtils.hasText(sleep.getStartTime()) && !StringUtils.hasText(sleep.getEndTime())) {
            stats.setOngoingCount(stats.getOngoingCount() + 1);
            durationSec = elapsedSeconds(sleep.getStartTime());
        }
        stats.setTotalDurationSec(stats.getTotalDurationSec() + durationSec);
        stats.setTotalDurationMin((stats.getTotalDurationSec() + 59) / 60);
    }

    /**
     * 将换尿布记录叠加到尿布统计（次数、各尿布类型数量、腹泻次数）
     * Accumulate diaper record into DiaperStats (count, counts by type, diarrhea count)
     */
    private void addDiaperStats(DailyStatsResponse.DiaperStats stats, DiaperRecordDto diaper) {
        stats.setCount(stats.getCount() + 1);
        String type = Optional.ofNullable(diaper.getType()).orElse("");
        if ("wet".equals(type)) stats.setWetCount(stats.getWetCount() + 1);
        if ("dirty".equals(type)) stats.setDirtyCount(stats.getDirtyCount() + 1);
        if ("both".equals(type)) stats.setBothCount(stats.getBothCount() + 1);
        if ("dry".equals(type)) stats.setDryCount(stats.getDryCount() + 1);
        boolean diarrhea = diaper.getDiarrhea() != null && !diaper.getDiarrhea().isEmpty();
        if (diarrhea) stats.setDiarrheaCount(stats.getDiarrheaCount() + 1);
        if (Boolean.TRUE.equals(diaper.getAbnormal()) || diarrhea) {
            stats.setAbnormalCount(stats.getAbnormalCount() + 1);
        }
    }

    /**
     * 将体温记录叠加到体温统计（次数、异常次数、最高体温）
     * Accumulate temperature record into TemperatureStats (count, abnormal count, max temperature)
     */
    private void addTemperatureStats(DailyStatsResponse.TemperatureStats stats, TemperatureRecordDto temperature) {
        stats.setCount(stats.getCount() + 1);
        if (Boolean.TRUE.equals(temperature.getIsAbnormal()) || isFever(temperature.getTemperature())) {
            stats.setAbnormalCount(stats.getAbnormalCount() + 1);
        }
        if (temperature.getTemperature() != null
                && (stats.getMaxTemperature() == null || temperature.getTemperature().compareTo(stats.getMaxTemperature()) > 0)) {
            stats.setMaxTemperature(temperature.getTemperature());
            stats.setMaxTemperatureTime(temperature.getTime());
        }
    }

    private void addDiaperAbnormalStats(DailyStatsResponse.AbnormalStats stats, DiaperRecordDto diaper) {
        if (diaper.getDiarrhea() == null || diaper.getDiarrhea().isEmpty()) {
            return;
        }
        stats.setCount(stats.getCount() + 1);
        stats.setDiarrheaCount(stats.getDiarrheaCount() + 1);
    }

    private void addTemperatureAbnormalStats(DailyStatsResponse.AbnormalStats stats, TemperatureRecordDto temperature) {
        if (!isFever(temperature.getTemperature())) {
            return;
        }
        stats.setCount(stats.getCount() + 1);
        stats.setFeverCount(stats.getFeverCount() + 1);
    }

    /**
     * 将补充剂/药品记录叠加到补充剂统计（总次数、药品次数、营养品次数）
     * Accumulate supplement record into SupplementStats (total count, medicine count, nutrition count)
     */
    private void addSupplementStats(DailyStatsResponse.SupplementStats stats, SupplementRecordDto supplement) {
        stats.setCount(stats.getCount() + 1);
        if ("medicine".equals(supplement.getType())) {
            stats.setMedicineCount(stats.getMedicineCount() + 1);
        } else {
            stats.setNutritionCount(stats.getNutritionCount() + 1);
        }
    }

    /**
     * 将生长记录叠加到生长统计（次数、最新身高体重）
     * Accumulate growth record into GrowthStats (count, latest height and weight)
     */
    private void addGrowthStats(DailyStatsResponse.GrowthStats stats, GrowthRecordDto growth) {
        stats.setCount(stats.getCount() + 1);
        if (stats.getLatestTime() == null || Optional.ofNullable(growth.getTime()).orElse("").compareTo(stats.getLatestTime()) >= 0) {
            stats.setLatestHeight(growth.getHeight());
            stats.setLatestWeight(growth.getWeight());
            stats.setLatestTime(growth.getTime());
        }
    }

    /**
     * 将吸奶记录叠加到吸奶统计（次数、总量、总时长）
     * Accumulate pump record into PumpStats (count, total amount, total duration)
     */
    private void addPumpStats(DailyStatsResponse.PumpStats stats, PumpRecordDto pump) {
        stats.setCount(stats.getCount() + 1);
        stats.setTotalAmount(stats.getTotalAmount() + safeInt(pump.getTotalAmount()));
        int durationSec = (safeInt(pump.getLeftDuration()) + safeInt(pump.getRightDuration())) * 60;
        stats.setTotalDurationSec(stats.getTotalDurationSec() + durationSec);
        stats.setTotalDurationMin((stats.getTotalDurationSec() + 59) / 60);
    }

    /**
     * 将辅食记录叠加到辅食统计（次数、异常次数）
     * Accumulate complementary food record into ComplementaryStats (count, abnormal count)
     */
    private void addComplementaryStats(DailyStatsResponse.ComplementaryStats stats, ComplementaryRecordDto complementary) {
        stats.setCount(stats.getCount() + 1);
        if (Boolean.TRUE.equals(complementary.getAbnormal())) {
            stats.setAbnormalCount(stats.getAbnormalCount() + 1);
        }
    }

    /**
     * 将异常记录叠加到异常统计（次数、发烧/腹泻/吕吐/用药次数）
     * Accumulate abnormal record into AbnormalStats (count, fever/diarrhea/vomit/medicine counts)
     */
    private void addMaternalFoodStats(DailyStatsResponse.MaternalFoodStats stats, MaternalFoodRecordDto maternalFood) {
        stats.setCount(stats.getCount() + 1);
        String level = maternalFood == null ? null : maternalFood.getSuspicionLevel();
        if (StringUtils.hasText(level) && !"none".equals(level)) {
            stats.setSuspectCount(stats.getSuspectCount() + 1);
        }
    }

    private void addAbnormalStats(DailyStatsResponse.AbnormalStats stats, AbnormalRecordDto abnormal) {
        if (isGeneratedDiarrheaMirror(abnormal)) {
            return;
        }
        stats.setCount(stats.getCount() + 1);
        if (isFever(abnormal.getTemperature())) {
            stats.setFeverCount(stats.getFeverCount() + 1);
        }
        if (abnormal.getDiarrhea() != null && !abnormal.getDiarrhea().isEmpty()) {
            stats.setDiarrheaCount(stats.getDiarrheaCount() + 1);
        }
        if (StringUtils.hasText(abnormal.getVomit())) {
            stats.setVomitCount(stats.getVomitCount() + 1);
        }
        if (abnormal.getMedicine() != null) {
            stats.setMedicineCount(stats.getMedicineCount() + 1);
        }
        if (hasOtherAbnormalSignal(abnormal)) {
            stats.setOtherCount(stats.getOtherCount() + 1);
        }
    }

    /**
     * 将活动记录叠加到活动统计（次数、总时长）
     * Accumulate activity record into ActivityStats (count, total duration)
     */
    private void addActivityStats(DailyStatsResponse.ActivityStats stats, ActivityRecordDto activity) {
        stats.setCount(stats.getCount() + 1);
        stats.setTotalDurationMin(stats.getTotalDurationMin() + safeInt(activity.getDuration()));
        stats.setTotalDurationSec(stats.getTotalDurationMin() * 60);
    }

    /**
     * 将单条 ChildRecord 按类型分散到 DailyRecordsResponse 的对应集合中
     * Scatter a single ChildRecord by type into the corresponding collection of DailyRecordsResponse
     */
    private void addToDailyResponse(DailyRecordsResponse response, ChildRecord record,
                                    Map<String, ChildRecord> recoveryMarkers) {
        switch (record.getRecordType()) {
            case "feed":
                response.getFeeds().add(readPayloadWithId(record, FeedRecordDto.class));
                break;
            case "diaper":
                DiaperRecordDto diaper = readPayloadWithId(record, DiaperRecordDto.class);
                if (diaper.getDiarrhea() != null && !diaper.getDiarrhea().isEmpty()) {
                    applyRecoveryStatus(diaper, record, recoveryMarkers.get("diarrhea_resolved"));
                }
                response.getDiapers().add(diaper);
                break;
            case "sleep":
                response.getSleeps().add(readSleepPayloadForDisplay(record));
                break;
            case "temperature":
                TemperatureRecordDto temperature = readPayloadWithId(record, TemperatureRecordDto.class);
                if (isFever(temperature.getTemperature())) {
                    applyRecoveryStatus(temperature, record, recoveryMarkers.get("fever_resolved"));
                }
                response.getTemperatures().add(temperature);
                break;
            case "supplement":
                response.getSupplements().add(readPayloadWithId(record, SupplementRecordDto.class));
                break;
            case "growth":
                response.getGrowths().add(readPayloadWithId(record, GrowthRecordDto.class));
                break;
            case "abnormal":
                AbnormalRecordDto abnormal = readPayloadWithId(record, AbnormalRecordDto.class);
                applyRecoveryStatus(abnormal, record, recoveryMarkers);
                response.getAbnormals().add(abnormal);
                break;
            case "pump":
                response.getPumps().add(readPayloadWithId(record, PumpRecordDto.class));
                break;
            case "complementary":
                response.getComplementaries().add(readPayloadWithId(record, ComplementaryRecordDto.class));
                break;
            case "maternal_food":
                response.getMaternalFoods().add(readPayloadWithId(record, MaternalFoodRecordDto.class));
                break;
            default:
                break;
        }
    }

    /**
     * 将 ChildRecord 的 payloadJson 反序列化为 DTO，并将 id 和 babyId 回写（如果 DTO 中为 null）
     * Deserialize ChildRecord payloadJson to DTO; write back id and babyId if null in DTO
     */
    private SleepRecordDto readSleepPayloadForDisplay(ChildRecord record) {
        SleepRecordDto sleep = readPayloadWithId(record, SleepRecordDto.class);
        List<ChildRecord> chain = findLinkedSleepChain(record);
        if (chain.size() <= 1) {
            return sleep;
        }

        SleepRecordDto first = readPayload(chain.get(0), SleepRecordDto.class);
        SleepRecordDto last = readPayload(chain.get(chain.size() - 1), SleepRecordDto.class);
        sleep.setDisplayStartTime(StringUtils.hasText(first.getStartTime()) ? first.getStartTime() : sleep.getStartTime());
        sleep.setDisplayEndTime(StringUtils.hasText(last.getEndTime()) ? last.getEndTime() : sleep.getEndTime());
        return sleep;
    }

    private <T extends BaseRecordDto> T readPayloadWithId(ChildRecord record, Class<T> payloadType) {
        T dto = readPayload(record, payloadType);
        if (dto.getId() == null) {
            dto.setId(record.getId());
        }
        if (dto.getBabyId() == null) {
            dto.setBabyId(record.getBabyId());
        }
        return dto;
    }

    /**
     * 将 ChildRecord 的 payloadJson 反序列化为指定类型的 DTO，失败时抛出 IllegalStateException
     * Deserialize ChildRecord payloadJson to specified DTO type; throw IllegalStateException on failure
     */
    private <T> T readPayload(ChildRecord record, Class<T> payloadType) {
        try {
            return objectMapper.readValue(record.getPayloadJson(), payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid record payload: " + record.getId(), e);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串，失败时抛出 IllegalStateException
     * Serialize object to JSON string; throw IllegalStateException on failure
     */
    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write record payload", e);
        }
    }

    /**
     * 将时间字符串解析为 LocalDateTime，如果为空则返回当前时间
     * Parse time string to LocalDateTime; return current time if empty
     */
    private LocalDateTime resolveRecordTime(String time) {
        return StringUtils.hasText(time) ? parseFrontTime(time) : LocalDateTime.now();
    }

    /**
     * 将前端时间字符串解析为 LocalDateTime，支持 yyyy-MM-dd HH:mm 和 yyyy-MM-dd HH:mm:ss 两种格式
     * Parse front-end time string to LocalDateTime; supports both "yyyy-MM-dd HH:mm" and "yyyy-MM-dd HH:mm:ss"
     */
    private LocalDateTime parseFrontTime(String time) {
        return time != null && time.length() >= 19
                ? LocalDateTime.parse(time, FRONT_TIME_SECONDS)
                : LocalDateTime.parse(time, FRONT_TIME);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将秒数和分钟数转换为秒，优先使用 seconds 字段，无效时尝试 minutes * 60
     * Convert seconds and minutes to seconds, preferring the seconds field; fallback to minutes * 60
     */
    private int durationSeconds(Integer seconds, Integer minutes) {
        if (seconds != null && seconds >= 0) {
            return seconds;
        }
        return minutes == null ? 0 : minutes * 60;
    }

    /**
     * 计算从给定开始时间到当前时间的已过秒数（用于进行中睡眠/摄奶时长计算）
     * Calculate elapsed seconds from given start time to now (used for ongoing sleep/pump duration)
     */
    private int elapsedSeconds(String startTime) {
        if (!StringUtils.hasText(startTime)) {
            return 0;
        }
        try {
            return Math.max(0, Math.toIntExact(Duration.between(parseFrontTime(startTime), LocalDateTime.now()).getSeconds()));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * 判断体温是否达到发烧阈唃（37.3℃ 及以上）
     * Check if the temperature reaches fever threshold (>= 37.3℃)
     */
    private boolean isFever(BigDecimal temperature) {
        return temperature != null && temperature.compareTo(FEVER_THRESHOLD) >= 0;
    }

    private List<ChildRecord> getHealthTrackingRecords(Long babyId) {
        return childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .in(ChildRecord::getRecordType, HEALTH_TRACKING_RECORD_TYPES)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));
    }

    private Map<String, ChildRecord> latestRecoveryMarkers(List<ChildRecord> records) {
        Map<String, ChildRecord> markers = new LinkedHashMap<>();
        markers.put("fever_resolved", latestRecord(records, "fever_resolved"));
        markers.put("diarrhea_resolved", latestRecord(records, "diarrhea_resolved"));
        markers.put("abnormal_resolved", latestRecord(records, "abnormal_resolved"));
        return markers;
    }

    private ChildRecord latestRecord(List<ChildRecord> records, String recordType) {
        return lastRecord(records.stream()
                .filter(record -> recordType.equals(record.getRecordType()))
                .collect(Collectors.toList()));
    }

    private ChildRecord lastRecord(List<ChildRecord> records) {
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    /**
     * 前端记录时间精确到分钟，恢复事件精确到秒。同一分钟内使用自增 ID 保证后写入的异常可以重新打开追踪。
     * Front-end record timestamps use minute precision; use the ID as a tie-breaker so a later event can reopen tracking.
     */
    private boolean isAfter(ChildRecord record, ChildRecord marker) {
        if (marker == null) {
            return true;
        }
        LocalDateTime recordTime = record.getRecordTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime markerTime = marker.getRecordTime().truncatedTo(ChronoUnit.MINUTES);
        int compared = recordTime.compareTo(markerTime);
        if (compared != 0) {
            return compared > 0;
        }
        return record.getId() != null
                && marker.getId() != null
                && record.getId() > marker.getId();
    }

    private boolean isResolvedBy(ChildRecord record, ChildRecord marker) {
        return marker != null && isAfter(marker, record);
    }

    private String resolvedTimeText(ChildRecord marker) {
        return FRONT_TIME.format(marker.getRecordTime());
    }

    private void applyRecoveryStatus(TemperatureRecordDto temperature, ChildRecord record, ChildRecord marker) {
        boolean resolved = isResolvedBy(record, marker);
        temperature.setResolved(resolved);
        temperature.setResolvedTime(resolved ? resolvedTimeText(marker) : null);
    }

    private void applyRecoveryStatus(DiaperRecordDto diaper, ChildRecord record, ChildRecord marker) {
        boolean resolved = isResolvedBy(record, marker);
        diaper.setResolved(resolved);
        diaper.setResolvedTime(resolved ? resolvedTimeText(marker) : null);
    }

    private void applyRecoveryStatus(AbnormalRecordDto abnormal, ChildRecord record,
                                     Map<String, ChildRecord> recoveryMarkers) {
        List<ChildRecord> requiredMarkers = new java.util.ArrayList<>();
        if (isFever(abnormal.getTemperature())) {
            requiredMarkers.add(recoveryMarkers.get("fever_resolved"));
        }
        if (abnormal.getDiarrhea() != null && !abnormal.getDiarrhea().isEmpty()) {
            requiredMarkers.add(recoveryMarkers.get("diarrhea_resolved"));
        }
        if (!isFever(abnormal.getTemperature()) && hasOtherAbnormalSignal(abnormal)) {
            requiredMarkers.add(recoveryMarkers.get("abnormal_resolved"));
        }
        if (requiredMarkers.isEmpty()) {
            return;
        }
        boolean resolved = requiredMarkers.stream().allMatch(marker -> isResolvedBy(record, marker));
        abnormal.setResolved(resolved);
        if (resolved) {
            ChildRecord latestMarker = requiredMarkers.stream()
                    .max(Comparator.comparing(ChildRecord::getRecordTime)
                            .thenComparing(ChildRecord::getId))
                    .orElse(null);
            abnormal.setResolvedTime(resolvedTimeText(latestMarker));
        }
    }

    private boolean isFeverRecord(ChildRecord record) {
        if ("abnormal".equals(record.getRecordType())) {
            return isFever(readPayloadWithId(record, AbnormalRecordDto.class).getTemperature());
        }
        if ("temperature".equals(record.getRecordType())) {
            return isFever(readPayloadWithId(record, TemperatureRecordDto.class).getTemperature());
        }
        return false;
    }

    private boolean isDiarrheaRecord(ChildRecord record) {
        return !diarrheaTypes(record).isEmpty();
    }

    private List<String> diarrheaTypes(ChildRecord record) {
        if ("abnormal".equals(record.getRecordType())) {
            return Optional.ofNullable(readPayloadWithId(record, AbnormalRecordDto.class).getDiarrhea())
                    .orElse(List.of());
        }
        if ("diaper".equals(record.getRecordType())) {
            return Optional.ofNullable(readPayloadWithId(record, DiaperRecordDto.class).getDiarrhea())
                    .orElse(List.of());
        }
        return List.of();
    }

    private boolean isOtherAbnormalRecord(ChildRecord record) {
        if (!"abnormal".equals(record.getRecordType())) {
            return false;
        }
        AbnormalRecordDto abnormal = readPayloadWithId(record, AbnormalRecordDto.class);
        return !isFever(abnormal.getTemperature()) && hasOtherAbnormalSignal(abnormal);
    }

    private String latestMedicineTime(List<ChildRecord> records) {
        ChildRecord latestMedicine = null;
        for (ChildRecord record : records) {
            if ("supplement".equals(record.getRecordType())
                    && "medicine".equals(readPayloadWithId(record, SupplementRecordDto.class).getType())) {
                latestMedicine = record;
            }
            if ("abnormal".equals(record.getRecordType())
                    && readPayloadWithId(record, AbnormalRecordDto.class).getMedicine() != null) {
                latestMedicine = record;
            }
        }
        return latestMedicine == null ? null : recordTimeText(latestMedicine);
    }

    private String recordTimeText(ChildRecord record) {
        if ("diaper".equals(record.getRecordType())) {
            String time = readPayloadWithId(record, DiaperRecordDto.class).getTime();
            if (StringUtils.hasText(time)) return time;
        }
        if ("abnormal".equals(record.getRecordType())) {
            String time = readPayloadWithId(record, AbnormalRecordDto.class).getTime();
            if (StringUtils.hasText(time)) return time;
        }
        if ("supplement".equals(record.getRecordType())) {
            String time = readPayloadWithId(record, SupplementRecordDto.class).getTime();
            if (StringUtils.hasText(time)) return time;
        }
        return FRONT_TIME.format(record.getRecordTime());
    }

    private FeverInfoDto toFeverInfo(ChildRecord record) {
        if ("temperature".equals(record.getRecordType())) {
            return toFeverInfo(readPayloadWithId(record, TemperatureRecordDto.class));
        }
        return toFeverInfo(readPayloadWithId(record, AbnormalRecordDto.class));
    }

    /**
     * 将 AbnormalRecordDto 转换为 FeverInfoDto（当异常记录包含发烧数据时使用）
     * Convert AbnormalRecordDto to FeverInfoDto (used when abnormal record contains fever data)
     */
    private FeverInfoDto toFeverInfo(AbnormalRecordDto abnormal) {
        FeverInfoDto feverInfo = new FeverInfoDto();
        feverInfo.setId(abnormal.getId());
        feverInfo.setTemperature(abnormal.getTemperature());
        feverInfo.setRespiratory(abnormal.getRespiratory());
        feverInfo.setDiarrhea(abnormal.getDiarrhea());
        feverInfo.setVomit(abnormal.getVomit());
        feverInfo.setOther(abnormal.getOther());
        feverInfo.setMedicine(abnormal.getMedicine());
        feverInfo.setNote(abnormal.getNote());
        feverInfo.setPhotos(abnormal.getPhotos());
        feverInfo.setTime(abnormal.getTime());
        return feverInfo;
    }

    /**
     * 将 TemperatureRecordDto 转换为 FeverInfoDto（当体温记录标记为异常且达到发烧阈唃时使用）
     * Convert TemperatureRecordDto to FeverInfoDto (used when temperature is marked abnormal and >= fever threshold)
     */
    private FeverInfoDto toFeverInfo(TemperatureRecordDto temperature) {
        FeverInfoDto feverInfo = new FeverInfoDto();
        feverInfo.setId(temperature.getId());
        feverInfo.setTemperature(temperature.getTemperature());
        feverInfo.setIsAbnormal(temperature.getIsAbnormal());
        feverInfo.setNote(temperature.getNote());
        feverInfo.setTime(temperature.getTime());
        return feverInfo;
    }

    private boolean hasOtherAbnormalSignal(AbnormalRecordDto abnormal) {
        if (abnormal == null) return false;
        if (isGeneratedDiarrheaMirror(abnormal)) {
            return false;
        }
        boolean hasDiarrhea = abnormal.getDiarrhea() != null && !abnormal.getDiarrhea().isEmpty();
        return StringUtils.hasText(abnormal.getOther())
                || (abnormal.getRespiratory() != null && !abnormal.getRespiratory().isEmpty())
                || StringUtils.hasText(abnormal.getVomit())
                || (StringUtils.hasText(abnormal.getNote()) && !hasDiarrhea)
                || abnormal.getMedicine() != null;
    }

    private boolean isGeneratedDiarrheaMirror(AbnormalRecordDto abnormal) {
        return abnormal != null
                && "换尿布时标记".equals(abnormal.getNote())
                && StringUtils.hasText(abnormal.getOther())
                && abnormal.getOther().startsWith("便便异常：");
    }

    /**
     * 计算两个时间点的差値，返回格式为X小时X分钟或X分钟的字符串
     * Calculate the time difference between two time points; return "X小时X分钟" or "X分钟" string
     */
    private String calcTimeDiff(LocalDateTime startTime, LocalDateTime endTime) {
        long diffMin = Math.max(0L, Duration.between(startTime, endTime).toMinutes());
        if (diffMin < 60L) {
            return diffMin + "分钟";
        }
        long hours = diffMin / 60L;
        long minutes = diffMin % 60L;
        return hours + "小时" + minutes + "分钟";
    }

    /**
     * 将总分钟数格式化为X小时X分钟字符串
     * Format total minutes as "X小时X分钟" string
     */
    private String formatMinutes(int totalSleepMin) {
        int hours = totalSleepMin / 60;
        int minutes = totalSleepMin % 60;
        return hours + "小时" + minutes + "分钟";
    }
}
