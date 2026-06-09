package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisAgent;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisRequest;
import com.ycz.childnotesbackend.agent.baby.BabyAnalysisResult;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.AiAnalysisRecordMapper;
import com.ycz.childnotesbackend.mapper.BabyMemberMapper;
import com.ycz.childnotesbackend.mapper.BabyMapper;
import com.ycz.childnotesbackend.mapper.ChildRecordMapper;
import com.ycz.childnotesbackend.model.dto.ai.AiAnalysisRecordDto;
import com.ycz.childnotesbackend.model.entity.AiAnalysisRecord;
import com.ycz.childnotesbackend.model.entity.Baby;
import com.ycz.childnotesbackend.model.entity.BabyMember;
import com.ycz.childnotesbackend.model.entity.ChildRecord;
import com.ycz.childnotesbackend.service.AiAnalysisService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private static final String SKILL_PATH = "skills/baby-ai-analysis-skill.txt";

    private static final int MAX_SOURCE_TEXT_CHARS = 60000;

    private static final int ANALYSIS_RANGE_DAYS = 7;

    private static final int SPARSE_RECORD_DAY_THRESHOLD = 4;

    private static final String DATA_QUALITY_TIP_PREFIX = "数据完整度提示: ";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final TypeReference<LinkedHashMap<String, Object>> PAYLOAD_TYPE =
            new TypeReference<LinkedHashMap<String, Object>>() {};

    private final AiAnalysisRecordMapper aiAnalysisRecordMapper;

    private final ChildRecordMapper childRecordMapper;

    private final BabyMapper babyMapper;

    private final BabyMemberMapper babyMemberMapper;

    private final BabyAnalysisAgent babyAnalysisAgent;

    private final ObjectMapper objectMapper;

    public AiAnalysisServiceImpl(AiAnalysisRecordMapper aiAnalysisRecordMapper,
                                 ChildRecordMapper childRecordMapper,
                                 BabyMapper babyMapper,
                                 BabyMemberMapper babyMemberMapper,
                                 BabyAnalysisAgent babyAnalysisAgent,
                                 ObjectMapper objectMapper) {
        this.aiAnalysisRecordMapper = aiAnalysisRecordMapper;
        this.childRecordMapper = childRecordMapper;
        this.babyMapper = babyMapper;
        this.babyMemberMapper = babyMemberMapper;
        this.babyAnalysisAgent = babyAnalysisAgent;
        this.objectMapper = objectMapper;
    }

    /**
     * 为当前宝宝生成一份 AI 分析报告（当日内只生成一次）
     * Generate an AI analysis report for the current baby (only once per day)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户和当前宝宝
     *    Get current user and current baby
     * 2. 检查今日是否已有分析记录，有则直接返回缓存结果
     *    Check if today's analysis already exists; return cached result if so
     * 3. 查询宝宝近一个月的所有成长记录
     *    Query all records for the baby in the past month
     * 4. 构建结构化文本（宝宝信息 + 汇总统计 + 记录明细），超长截断
     *    Build structured source text (baby info + aggregate stats + record detail), truncate if too long
     * 5. 加载技能提示词（skills/ 目录下的 txt 文件）
     *    Load skill prompt from classpath resources
     * 6. 调用宝宝分析智能体分析记录
     *    Call baby analysis agent to analyze records
     * 7. 将分析结果持久化到数据库（处理并发重复插入的幾率冲突）
     *    Persist analysis result to DB (handle concurrent duplicate key race condition)
     * 8. 返回分析报告 DTO
     *    Return analysis report DTO
     *
     * @return AI 分析记录 DTO / AI analysis record DTO
     */
    @Override
    public AiAnalysisRecordDto generateCurrentBabyAnalysis() {
        return generateCurrentBabyAnalysis(null, null);
    }

    @Override
    public AiAnalysisRecordDto generateCurrentBabyAnalysis(String startDate, String endDate) {
        LocalDate[] range = resolveAnalysisRange(startDate, endDate);
        return generateCurrentBabyAnalysisForRange(range[0], range[1]);
    }

    private AiAnalysisRecordDto generateCurrentBabyAnalysisForRange(LocalDate startDate, LocalDate endDate) {
        Long userId = currentUserId();
        Baby baby = currentBaby(userId);
        List<ChildRecord> records = listRecords(baby.getId(), startDate, endDate);
        AnalysisComparison comparison = buildAnalysisComparison(userId, baby.getId(), startDate, endDate, records);
        String sourceText = buildSourceText(baby, startDate, endDate, records, comparison);
        AiAnalysisRecord rangeRecord = findAnalysisByRange(userId, baby.getId(), startDate, endDate);
        if (rangeRecord != null && Objects.equals(rangeRecord.getSourceText(), sourceText)) {
            return toDto(rangeRecord);
        }
        String skillPrompt = loadSkillPrompt();
        BabyAnalysisResult analysisResult = babyAnalysisAgent.analyze(new BabyAnalysisRequest(
                userId,
                baby.getId(),
                baby.getName(),
                startDate,
                endDate,
                skillPrompt,
                sourceText
        ));

        LocalDateTime now = LocalDateTime.now();
        if (rangeRecord != null) {
            applyAnalysisContent(rangeRecord, baby, startDate, endDate, sourceText, skillPrompt, analysisResult, now);
            aiAnalysisRecordMapper.updateById(rangeRecord);
            return toDto(rangeRecord);
        }

        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setUserId(userId);
        record.setBabyId(baby.getId());
        record.setCreatedAt(now);
        applyAnalysisContent(record, baby, startDate, endDate, sourceText, skillPrompt, analysisResult, now);
        try {
            aiAnalysisRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            AiAnalysisRecord existing = findAnalysisByRange(userId, baby.getId(), startDate, endDate);
            if (existing != null) {
                if (Objects.equals(existing.getSourceText(), sourceText)) {
                    return toDto(existing);
                }
                applyAnalysisContent(existing, baby, startDate, endDate, sourceText, skillPrompt, analysisResult, now);
                aiAnalysisRecordMapper.updateById(existing);
                return toDto(existing);
            }
            AiAnalysisRecord sameEndDateRecord = findAnalysisByEndDate(userId, baby.getId(), endDate);
            if (sameEndDateRecord != null) {
                applyAnalysisContent(sameEndDateRecord, baby, startDate, endDate, sourceText, skillPrompt, analysisResult, now);
                aiAnalysisRecordMapper.updateById(sameEndDateRecord);
                return toDto(sameEndDateRecord);
            }
            throw e;
        }
        return toDto(record);
    }

    private void applyAnalysisContent(AiAnalysisRecord record,
                                      Baby baby,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      String sourceText,
                                      String skillPrompt,
                                      BabyAnalysisResult analysisResult,
                                      LocalDateTime updatedAt) {
        record.setBabyName(baby.getName());
        record.setRangeStartDate(startDate);
        record.setRangeEndDate(endDate);
        record.setSourceText(sourceText);
        record.setSkillPrompt(skillPrompt);
        record.setAnalysisText(analysisResult.getAnalysisText());
        record.setModel(analysisResult.getModel());
        record.setUpdatedAt(updatedAt);
    }

    private LocalDate[] resolveAnalysisRange(String rawStartDate, String rawEndDate) {
        boolean hasStartDate = StringUtils.hasText(rawStartDate);
        boolean hasEndDate = StringUtils.hasText(rawEndDate);
        if (!hasStartDate && !hasEndDate) {
            LocalDate endDate = LocalDate.now();
            return new LocalDate[] { endDate.minusDays(ANALYSIS_RANGE_DAYS - 1L), endDate };
        }
        if (!hasStartDate || !hasEndDate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择完整的分析时间范围");
        }

        LocalDate startDate = parseDate(rawStartDate, "startDate");
        LocalDate endDate = parseDate(rawEndDate, "endDate");
        if (endDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分析结束日期不能晚于今天");
        }
        long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (rangeDays != ANALYSIS_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "宝宝喂养分析仅支持连续7天数据");
        }
        return new LocalDate[] { startDate, endDate };
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName + ": " + value, e);
        }
    }

    private AiAnalysisRecord findAnalysisByRange(Long userId, Long babyId, LocalDate startDate, LocalDate endDate) {
        return aiAnalysisRecordMapper.selectOne(new LambdaQueryWrapper<AiAnalysisRecord>()
                .eq(AiAnalysisRecord::getUserId, userId)
                .eq(AiAnalysisRecord::getBabyId, babyId)
                .eq(AiAnalysisRecord::getRangeStartDate, startDate)
                .eq(AiAnalysisRecord::getRangeEndDate, endDate)
                .orderByDesc(AiAnalysisRecord::getCreatedAt)
                .orderByDesc(AiAnalysisRecord::getId)
                .last("limit 1"));
    }

    private AiAnalysisRecord findAnalysisByEndDate(Long userId, Long babyId, LocalDate endDate) {
        return aiAnalysisRecordMapper.selectOne(new LambdaQueryWrapper<AiAnalysisRecord>()
                .eq(AiAnalysisRecord::getUserId, userId)
                .eq(AiAnalysisRecord::getBabyId, babyId)
                .eq(AiAnalysisRecord::getRangeEndDate, endDate)
                .orderByDesc(AiAnalysisRecord::getCreatedAt)
                .orderByDesc(AiAnalysisRecord::getId)
                .last("limit 1"));
    }

    private AiAnalysisRecord findPreviousAnalysis(Long userId, Long babyId, LocalDate startDate) {
        return aiAnalysisRecordMapper.selectOne(new LambdaQueryWrapper<AiAnalysisRecord>()
                .eq(AiAnalysisRecord::getUserId, userId)
                .eq(AiAnalysisRecord::getBabyId, babyId)
                .lt(AiAnalysisRecord::getRangeEndDate, startDate)
                .orderByDesc(AiAnalysisRecord::getRangeEndDate)
                .orderByDesc(AiAnalysisRecord::getCreatedAt)
                .orderByDesc(AiAnalysisRecord::getId)
                .last("limit 1"));
    }

    /**
     * 获取当前宝宝的 AI 分析历史列表，按创建时间倒序排序
     * Get AI analysis history list for the current baby, ordered by creation time DESC
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户和当前宝宝
     *    Get current user and baby
     * 2. 查询该用户对宝宝的所有分析记录，按时间倒序返回
     *    Query all analysis records for this user+baby, ordered by createdAt DESC
     * 3. 转换为 DTO 列表返回
     *    Convert to DTO list and return
     *
     * @return AI 分析记录 DTO 列表 / list of AI analysis record DTOs
     */
    @Override
    public List<AiAnalysisRecordDto> listCurrentBabyAnalyses() {
        Long userId = currentUserId();
        Baby baby = currentBaby(userId);
        return aiAnalysisRecordMapper.selectList(new LambdaQueryWrapper<AiAnalysisRecord>()
                        .eq(AiAnalysisRecord::getUserId, userId)
                        .eq(AiAnalysisRecord::getBabyId, baby.getId())
                        .orderByDesc(AiAnalysisRecord::getCreatedAt)
                        .orderByDesc(AiAnalysisRecord::getId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定 AI 分析记录的详情
     * Get the detail of a specified AI analysis record
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 根据 ID 查询分析记录，不存在或不属于当前用户则 404
     *    Find record by ID; 404 if not found or not owned by current user
     * 2. 验证当前用户对当该宝宝的访问权限
     *    Verify current user has access to the baby this record belongs to
     * 3. 返回分析记录 DTO
     *    Return analysis record DTO
     *
     * @param id 分析记录ID / analysis record ID
     * @return AI 分析记录 DTO / AI analysis record DTO
     */
    @Override
    public AiAnalysisRecordDto getAnalysisDetail(Long id) {
        Long userId = currentUserId();
        AiAnalysisRecord record = aiAnalysisRecordMapper.selectOne(new LambdaQueryWrapper<AiAnalysisRecord>()
                .eq(AiAnalysisRecord::getId, id)
                .eq(AiAnalysisRecord::getUserId, userId)
                .last("limit 1"));
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found: " + id);
        }
        if (findBabyForUser(record.getBabyId(), userId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Baby not found: " + record.getBabyId());
        }
        return toDto(record);
    }

    /**
     * 获取当前登录用户ID
     * Get the current logged-in user ID from AuthContext
     */
    private Long currentUserId() {
        return AuthContext.requireCurrentUserId();
    }

    /**
     * 获取当前宝宝实体：优先使用请求中指定的 babyId，否则取用户第一个宝宝
     * Get the current baby entity: prefer the requested babyId, otherwise find user's first baby
     * <p>
     * 如果找不到宝宝或没有访问权限则抛出 404
     * Throws 404 if baby not found or user has no access
     */
    private Baby currentBaby(Long userId) {
        Long requestedBabyId = getRequestedBabyId();
        Baby baby = requestedBabyId == null
                ? findFirstBabyForUser(userId)
                : findBabyForUser(requestedBabyId, userId);
        if (baby == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Baby not found: " + (requestedBabyId == null ? "current" : requestedBabyId)
            );
        }
        return baby;
    }

    /**
     * 查找用户有访问权的第一个宝宝：优先家庭成员表，其次直接所有
     * Find first baby accessible to user: prefer membership table, fallback to owned babies
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
     * 查找用户有权访问的指定宝宝：家庭成员记录存在或用户是宝宝主人
     * Find specific baby accessible to user: active membership exists or user is baby owner
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
     * 从 HTTP 请求头 X-Baby-Id 或查询参数中解析用户指定的 babyId
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid babyId: " + raw);
        }
    }

    /**
     * 查询指定宝宝在给定日期范围内的所有成长记录，按日期和时间升序
     * Query all child records for the baby in the specified date range, ordered by date and time ASC
     */
    private List<ChildRecord> listRecords(Long babyId, LocalDate startDate, LocalDate endDate) {
        return childRecordMapper.selectList(new LambdaQueryWrapper<ChildRecord>()
                .eq(ChildRecord::getBabyId, babyId)
                .between(ChildRecord::getRecordDate, startDate, endDate)
                .orderByAsc(ChildRecord::getRecordDate)
                .orderByAsc(ChildRecord::getRecordTime)
                .orderByAsc(ChildRecord::getId));
    }

    /**
     * 构建发送给 AI 的结构化源文本
     * Build the structured source text to be sent to the AI model
     * <p>
     * 文本包含三个部分 / Text includes three sections:
     * - 一、宝宝基本信息（姓名、性别、生日、年龄、分析区间）
     *   Section 1: Basic baby info (name, gender, birthdate, age, analysis period)
     * - 二、汇总统计（各类型记录数量、喂养量、睡眠时长等）
     *   Section 2: Aggregate statistics (record counts by type, feed amounts, sleep duration, etc.)
     * - 三、记录明细（每条记录的格式化文本）
     *   Section 3: Record detail (formatted text for each record)
     * 文本超过 60000 字符时截断并添加提示
     *   Truncated with prompt if text exceeds 60000 characters
     */
    private String buildSourceText(Baby baby,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   List<ChildRecord> records,
                                   AnalysisComparison comparison) {
        StringBuilder sb = new StringBuilder(12000);
        int recordDays = countRecordDays(records);
        String dataQualityTip = buildDataQualityTip(recordDays);
        sb.append("宝宝所选连续7天记录 TXT\n\n");
        sb.append("一、宝宝信息\n");
        appendLine(sb, "宝宝姓名", baby.getName());
        appendLine(sb, "宝宝性别", formatGender(baby.getGender()));
        appendLine(sb, "出生日期", baby.getBirthDate() == null ? "未填写" : baby.getBirthDate());
        appendLine(sb, "当前年龄", formatBabyAge(baby.getBirthDate()));
        appendLine(sb, "分析区间", startDate + " 至 " + endDate);
        appendLine(sb, "分析天数", ANALYSIS_RANGE_DAYS + "天");
        appendLine(sb, "有记录天数", recordDays + "/" + ANALYSIS_RANGE_DAYS);
        appendLine(sb, "记录总数", records.size());
        if (StringUtils.hasText(dataQualityTip)) {
            appendLine(sb, "数据完整度提示", dataQualityTip);
        }

        sb.append("\n二、汇总统计\n");
        appendAggregateSummary(sb, records);

        appendComparisonSummary(sb, comparison);

        sb.append("\n四、记录明细\n");
        if (records.isEmpty()) {
            sb.append("本区间暂无记录。\n");
        } else {
            records.forEach(record -> sb.append(formatRecordLine(record)).append('\n'));
        }

        return trimSourceText(sb.toString());
    }

    private int countRecordDays(List<ChildRecord> records) {
        return (int) records.stream()
                .map(ChildRecord::getRecordDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private String buildDataQualityTip(int recordDays) {
        if (recordDays >= SPARSE_RECORD_DAY_THRESHOLD) {
            return "";
        }
        if (recordDays <= 0) {
            return "所选7天暂无记录，数据较少，分析可能无法达到最精确。建议连续记录喂养、睡眠、尿布等关键数据。";
        }
        return "所选7天中只有 " + recordDays + " 天有记录，数据较少，分析可能无法达到最精确。建议连续记录喂养、睡眠、尿布等关键数据。";
    }

    private AnalysisComparison buildAnalysisComparison(Long userId,
                                                       Long babyId,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       List<ChildRecord> currentRecords) {
        AiAnalysisRecord previousAnalysis = findPreviousAnalysis(userId, babyId, startDate);
        LocalDate previousStartDate = previousAnalysis != null && previousAnalysis.getRangeStartDate() != null
                ? previousAnalysis.getRangeStartDate()
                : startDate.minusDays(ANALYSIS_RANGE_DAYS);
        LocalDate previousEndDate = previousAnalysis != null && previousAnalysis.getRangeEndDate() != null
                ? previousAnalysis.getRangeEndDate()
                : startDate.minusDays(1);
        List<ChildRecord> previousRecords = listRecords(babyId, previousStartDate, previousEndDate);
        return new AnalysisComparison(
                previousAnalysis,
                buildAnalysisMetrics(startDate, endDate, currentRecords),
                buildAnalysisMetrics(previousStartDate, previousEndDate, previousRecords)
        );
    }

    private AnalysisMetrics buildAnalysisMetrics(LocalDate startDate, LocalDate endDate, List<ChildRecord> records) {
        List<ChildRecord> feeds = filterByType(records, "feed");
        int bottleMilkMl = feeds.stream()
                .filter(record -> !"breast".equals(record.getRecordSubType()))
                .mapToInt(record -> safeInt(record.getAmountMl()))
                .sum();
        int breastDurationSec = feeds.stream()
                .filter(record -> "breast".equals(record.getRecordSubType()))
                .mapToInt(record -> safeInt(record.getDurationSec()))
                .sum();
        List<ChildRecord> sleeps = filterByType(records, "sleep");
        List<ChildRecord> diapers = filterByType(records, "diaper");
        int abnormalCount = (int) records.stream()
                .filter(record -> Boolean.TRUE.equals(record.getAbnormalFlag()) || "abnormal".equals(record.getRecordType()))
                .count();
        return new AnalysisMetrics(
                startDate,
                endDate,
                countRecordDays(records),
                records.size(),
                feeds.size(),
                bottleMilkMl,
                breastDurationSec,
                sleeps.size(),
                sleeps.stream().mapToInt(record -> safeInt(record.getDurationSec())).sum(),
                diapers.size(),
                abnormalCount
        );
    }

    private void appendComparisonSummary(StringBuilder sb, AnalysisComparison comparison) {
        sb.append("\n三、与上次分析对比\n");
        AnalysisMetrics current = comparison.currentMetrics;
        AnalysisMetrics previous = comparison.previousMetrics;
        appendLine(sb, "本次分析区间", current.startDate + " 至 " + current.endDate);
        appendLine(sb, "对比区间", previous.startDate + " 至 " + previous.endDate);
        appendLine(sb, "对比来源", comparison.previousAnalysis == null
                ? "暂无上一次分析记录，使用所选区间前一段连续7天原始记录作为上周基准"
                : "上一次分析记录（ID=" + comparison.previousAnalysis.getId() + "）");

        if (!previous.hasData()) {
            appendLine(sb, "对比提示", "上一次分析/上周基准区间暂无记录，本次只能描述当前7天情况，不能得出可靠变化趋势。");
        }

        appendNumberComparison(sb, "有记录天数", current.recordDays, previous.recordDays, "天");
        appendNumberComparison(sb, "记录总数", current.totalRecords, previous.totalRecords, "条");
        appendNumberComparison(sb, "喂养次数", current.feedCount, previous.feedCount, "次");
        appendNumberComparison(sb, "奶瓶/瓶喂总量", current.bottleMilkMl, previous.bottleMilkMl, "ml");
        appendDurationComparison(sb, "亲喂总时长", current.breastDurationSec, previous.breastDurationSec);
        appendNumberComparison(sb, "睡眠次数", current.sleepCount, previous.sleepCount, "次");
        appendDurationComparison(sb, "睡眠总时长", current.sleepDurationSec, previous.sleepDurationSec);
        appendNumberComparison(sb, "尿布/排便次数", current.diaperCount, previous.diaperCount, "次");
        appendNumberComparison(sb, "异常/症状记录", current.abnormalCount, previous.abnormalCount, "条");

        if (comparison.previousAnalysis != null && StringUtils.hasText(comparison.previousAnalysis.getAnalysisText())) {
            appendLine(sb, "上一次分析摘要", abbreviate(comparison.previousAnalysis.getAnalysisText(), 260));
        }
    }

    private void appendNumberComparison(StringBuilder sb, String label, int current, int previous, String unit) {
        appendLine(sb, label, "本次=" + current + unit
                + "；上次=" + previous + unit
                + "；变化=" + formatNumberDelta(current - previous, unit));
    }

    private void appendDurationComparison(StringBuilder sb, String label, int currentSec, int previousSec) {
        appendLine(sb, label, "本次=" + formatDuration(currentSec)
                + "；上次=" + formatDuration(previousSec)
                + "；变化=" + formatDurationDelta(currentSec - previousSec));
    }

    private String formatNumberDelta(int delta, String unit) {
        if (delta == 0) {
            return "持平";
        }
        return (delta > 0 ? "+" : "") + delta + unit;
    }

    private String formatDurationDelta(int deltaSec) {
        if (deltaSec == 0) {
            return "持平";
        }
        return (deltaSec > 0 ? "+" : "-") + formatDuration(Math.abs(deltaSec));
    }

    /**
     * 逐类型计算各项汇总统计并写入 StringBuilder
     * Calculate aggregate statistics by record type and write into StringBuilder
     * <p>
     * 包含：各类型记录数、喂养量、睡眠时长、尿布类型分布、最高体温、最新身高体重、异常记录数
     * Includes: counts by type, feed amounts, sleep duration, diaper type distribution,
     * max temperature, latest height/weight, abnormal record count
     */
    private void appendAggregateSummary(StringBuilder sb, List<ChildRecord> records) {
        Map<String, Integer> countByType = new LinkedHashMap<>();
        for (ChildRecord record : records) {
            countByType.merge(record.getRecordType(), 1, Integer::sum);
        }
        if (countByType.isEmpty()) {
            sb.append("暂无可统计记录。\n");
            return;
        }
        appendLine(sb, "各类型记录数", countByType.entrySet().stream()
                .map(entry -> recordTypeLabel(entry.getKey()) + "=" + entry.getValue())
                .collect(Collectors.joining("；")));

        List<ChildRecord> feeds = filterByType(records, "feed");
        if (!feeds.isEmpty()) {
            int bottleMilk = feeds.stream()
                    .filter(record -> !"breast".equals(record.getRecordSubType()))
                    .mapToInt(record -> safeInt(record.getAmountMl()))
                    .sum();
            int breastDurationSec = feeds.stream()
                    .filter(record -> "breast".equals(record.getRecordSubType()))
                    .mapToInt(record -> safeInt(record.getDurationSec()))
                    .sum();
            appendLine(sb, "喂养", "次数=" + feeds.size()
                    + "；奶瓶/瓶喂总量=" + bottleMilk + "ml"
                    + "；亲喂总时长=" + formatDuration(breastDurationSec));
        }

        List<ChildRecord> sleeps = filterByType(records, "sleep");
        if (!sleeps.isEmpty()) {
            int totalSleepSec = sleeps.stream().mapToInt(record -> safeInt(record.getDurationSec())).sum();
            appendLine(sb, "睡眠", "次数=" + sleeps.size() + "；总时长=" + formatDuration(totalSleepSec));
        }

        List<ChildRecord> diapers = filterByType(records, "diaper");
        if (!diapers.isEmpty()) {
            Map<String, Long> diaperTypes = diapers.stream()
                    .collect(Collectors.groupingBy(
                            record -> subTypeLabel(record.getRecordSubType()),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
            appendLine(sb, "尿布/排便", diaperTypes.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("；")));
        }

        BigDecimal maxTemperature = null;
        String maxTemperatureTime = "";
        for (ChildRecord record : records) {
            if (record.getTemperatureValue() != null
                    && (maxTemperature == null || record.getTemperatureValue().compareTo(maxTemperature) > 0)) {
                maxTemperature = record.getTemperatureValue();
                maxTemperatureTime = formatDateTime(record);
            }
        }
        if (maxTemperature != null) {
            appendLine(sb, "最高体温", maxTemperature.toPlainString() + "℃（" + maxTemperatureTime + "）");
        }

        ChildRecord latestGrowth = null;
        for (ChildRecord record : filterByType(records, "growth")) {
            if (latestGrowth == null || compareTime(record, latestGrowth) >= 0) {
                latestGrowth = record;
            }
        }
        if (latestGrowth != null) {
            List<String> growthItems = new ArrayList<>();
            if (latestGrowth.getHeightCm() != null) growthItems.add("身高=" + latestGrowth.getHeightCm().toPlainString() + "cm");
            if (latestGrowth.getWeightKg() != null) growthItems.add("体重=" + latestGrowth.getWeightKg().toPlainString() + "kg");
            if (!growthItems.isEmpty()) {
                appendLine(sb, "最新生长", String.join("；", growthItems) + "（" + formatDateTime(latestGrowth) + "）");
            }
        }

        int abnormalCount = (int) records.stream()
                .filter(record -> Boolean.TRUE.equals(record.getAbnormalFlag()) || "abnormal".equals(record.getRecordType()))
                .count();
        if (abnormalCount > 0) {
            appendLine(sb, "异常/症状记录", abnormalCount + "条");
        }

        List<ChildRecord> pumps = filterByType(records, "pump");
        if (!pumps.isEmpty()) {
            int pumpAmount = pumps.stream().mapToInt(record -> safeInt(record.getAmountMl())).sum();
            appendLine(sb, "吸奶", "次数=" + pumps.size() + "；总量=" + pumpAmount + "ml");
        }

        List<ChildRecord> activities = filterByType(records, "activity");
        if (!activities.isEmpty()) {
            int activityDurationSec = activities.stream().mapToInt(record -> safeInt(record.getDurationSec())).sum();
            appendLine(sb, "活动", "次数=" + activities.size() + "；总时长=" + formatDuration(activityDurationSec));
        }
    }

    /**
     * 按类型过滤记录列表
     * Filter records list by record type
     */
    private List<ChildRecord> filterByType(List<ChildRecord> records, String type) {
        return records.stream()
                .filter(record -> type.equals(record.getRecordType()))
                .collect(Collectors.toList());
    }

    /**
     * 将单条 ChildRecord 格式化为 AI 源文本中的明细行
     * Format a single ChildRecord as a detail line in the AI source text
     * <p>
     * 格式：- 时间 | 类型(子类型) | 数量/时长/体温等 | payload 补充信息
     * Format: - time | type(subtype) | amount/duration/temperature etc. | payload info
     */
    private String formatRecordLine(ChildRecord record) {
        List<String> summary = new ArrayList<>();
        if (record.getAmountMl() != null) summary.add("量=" + record.getAmountMl() + "ml");
        if (record.getDurationSec() != null && record.getDurationSec() > 0) summary.add("时长=" + formatDuration(record.getDurationSec()));
        if (record.getTemperatureValue() != null) summary.add("体温=" + record.getTemperatureValue().toPlainString() + "℃");
        if (record.getHeightCm() != null) summary.add("身高=" + record.getHeightCm().toPlainString() + "cm");
        if (record.getWeightKg() != null) summary.add("体重=" + record.getWeightKg().toPlainString() + "kg");
        if (Boolean.TRUE.equals(record.getAbnormalFlag())) summary.add("异常标记=是");

        StringBuilder line = new StringBuilder();
        line.append("- ").append(formatDateTime(record));
        line.append(" | ").append(recordTypeLabel(record.getRecordType()));
        if (StringUtils.hasText(record.getRecordSubType())) {
            line.append("(").append(subTypeLabel(record.getRecordSubType())).append(")");
        }
        if (!summary.isEmpty()) {
            line.append(" | ").append(String.join("；", summary));
        }
        String payload = payloadSummary(record.getPayloadJson());
        if (StringUtils.hasText(payload)) {
            line.append(" | ").append(payload);
        }
        return line.toString();
    }

    /**
     * 将记录的 payloadJson 解析并提取关键字段作为补充摘要字符串
     * Parse the record's payloadJson and extract key fields as a summary supplement string
     * <p>
     * 最多提取 24 个字段，自动跳过 id 和 babyId；解析失败时将 JSON 简化输出
     * Extracts up to 24 fields, skipping id/babyId; falls back to abbreviated raw JSON on failure
     */
    private String payloadSummary(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return "";
        }
        try {
            LinkedHashMap<String, Object> payload = objectMapper.readValue(payloadJson, PAYLOAD_TYPE);
            List<String> items = new ArrayList<>();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key) || "babyId".equals(key)) {
                    continue;
                }
                String value = formatPayloadValue(key, entry.getValue());
                if (StringUtils.hasText(value)) {
                    items.add(key + "=" + value);
                }
                if (items.size() >= 24) {
                    break;
                }
            }
            return String.join("；", items);
        } catch (Exception e) {
            return abbreviate(payloadJson, 240);
        }
    }

    /**
     * 将 payload 字段的单个值格式化为可读字符串
     * Format a single payload field value as a readable string
     * <p>
     * 图片显示张数，Map展开键值对，Collection展开元素，过长则截断
     * Images show count; Maps expand key-value pairs; Collections expand elements; long strings are abbreviated
     */
    private String formatPayloadValue(String key, Object value) {
        if (value == null) {
            return "";
        }
        if ("photos".equals(key) && value instanceof Collection) {
            return "图片" + ((Collection<?>) value).size() + "张";
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            return map.entrySet().stream()
                    .limit(12)
                    .map(entry -> entry.getKey() + ":" + formatPayloadValue(String.valueOf(entry.getKey()), entry.getValue()))
                    .collect(Collectors.joining(","));
        }
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                    .limit(12)
                    .map(item -> formatPayloadValue("", item))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(","));
        }
        return abbreviate(String.valueOf(value), 160);
    }

    /**
     * 从 classpath 中加载 AI 技能提示词文件内容
     * Load AI skill prompt text from classpath resource file
     * <p>
     * 文件路径为 skills/baby-ai-analysis-skill.txt，读取失败抛出 500
     * File path: skills/baby-ai-analysis-skill.txt; throws 500 on load failure
     */
    private String loadSkillPrompt() {
        ClassPathResource resource = new ClassPathResource(SKILL_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Analysis skill not found", e);
        }
    }

    /**
     * 将 AiAnalysisRecord 实体转换为 DTO，隐藏 sourceText 和 skillPrompt 等敏感大字段
     * Convert AiAnalysisRecord entity to DTO, hiding sensitive/large fields like sourceText and skillPrompt
     */
    private AiAnalysisRecordDto toDto(AiAnalysisRecord record) {
        AiAnalysisRecordDto dto = new AiAnalysisRecordDto();
        dto.setId(record.getId());
        dto.setBabyId(record.getBabyId());
        dto.setBabyName(record.getBabyName());
        dto.setRangeStartDate(record.getRangeStartDate() == null ? null : record.getRangeStartDate().toString());
        dto.setRangeEndDate(record.getRangeEndDate() == null ? null : record.getRangeEndDate().toString());
        dto.setAnalysisText(record.getAnalysisText());
        dto.setDataQualityTip(extractDataQualityTip(record.getSourceText()));
        dto.setModel(record.getModel());
        dto.setCreatedAt(formatDateTime(record.getCreatedAt()));
        dto.setUpdatedAt(formatDateTime(record.getUpdatedAt()));
        return dto;
    }

    private String extractDataQualityTip(String sourceText) {
        if (!StringUtils.hasText(sourceText)) {
            return "";
        }
        String[] lines = sourceText.replace("\r", "").split("\n");
        for (String line : lines) {
            if (line.startsWith(DATA_QUALITY_TIP_PREFIX)) {
                return line.substring(DATA_QUALITY_TIP_PREFIX.length()).trim();
            }
        }
        return "";
    }

    /**
     * 向 StringBuilder 追加一行格式化的标签-值对
     * Append a formatted "label: value" line to the StringBuilder
     */
    private void appendLine(StringBuilder sb, String label, Object value) {
        sb.append(label).append(": ").append(value == null ? "" : value).append('\n');
    }

    /**
     * 如果源文本超过最大字符限制，则截断并添加截断提示
     * Truncate source text if it exceeds the max character limit, appending a truncation notice
     */
    private String trimSourceText(String sourceText) {
        if (sourceText.length() <= MAX_SOURCE_TEXT_CHARS) {
            return sourceText;
        }
        return sourceText.substring(0, MAX_SOURCE_TEXT_CHARS)
                + "\n\n[后端提示：原始记录较多，后续明细已截断，以上统计汇总仍保留。]\n";
    }

    /**
     * 将性别字符串转换为中文显示（女宝/男宝/原値）
     * Convert gender string to Chinese display (女宝/男宝/original value)
     */
    private String formatGender(String gender) {
        if ("girl".equals(gender)) {
            return "女宝";
        }
        if ("boy".equals(gender)) {
            return "男宝";
        }
        return StringUtils.hasText(gender) ? gender : "未填写";
    }

    /**
     * 根据出生日期计算宝宝年龄并返回可读字符串
     * Calculate baby's age from birthDate and return a human-readable string
     * <p>
     * 格式：< 30天显示X天；< 12个月显示X个月X天；否则显示X岁X个月
     * Format: <30 days show X天; <12 months show X个月X天; otherwise X岁X个月
     */
    private String formatBabyAge(LocalDate birthDate) {
        if (birthDate == null) {
            return "未填写";
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(birthDate, LocalDate.now()));
        if (days < 30) {
            return days + "天";
        }
        long months = days / 30;
        long restDays = days % 30;
        if (months < 12) {
            return months + "个月" + restDays + "天";
        }
        return (months / 12) + "岁" + (months % 12) + "个月";
    }

    /**
     * 将记录类型编码转换为中文标签（用于 AI 源文本）
     * Convert record type code to Chinese label (used in AI source text)
     */
    private String recordTypeLabel(String type) {
        if ("feed".equals(type)) return "喂养";
        if ("diaper".equals(type)) return "尿布/排便";
        if ("sleep".equals(type)) return "睡眠";
        if ("temperature".equals(type)) return "体温";
        if ("supplement".equals(type)) return "营养/用药";
        if ("growth".equals(type)) return "生长";
        if ("abnormal".equals(type)) return "异常症状";
        if ("pump".equals(type)) return "吸奶";
        if ("complementary".equals(type)) return "辅食";
        if ("maternal_food".equals(type)) return "妈妈饮食观察";
        if ("vaccine".equals(type)) return "疫苗";
        if ("activity".equals(type)) return "活动";
        if ("milestone".equals(type)) return "里程碑";
        if ("fever_resolved".equals(type)) return "发热已处理";
        if ("diarrhea_resolved".equals(type)) return "腹泻已处理";
        return StringUtils.hasText(type) ? type : "未知";
    }

    /**
     * 将子类型编码转换为中文标签
     * Convert sub-type code to Chinese label
     */
    private String subTypeLabel(String subType) {
        if ("breast".equals(subType)) return "亲喂";
        if ("bottle".equals(subType)) return "奶瓶";
        if ("expressed".equals(subType)) return "瓶喂母乳";
        if ("wet".equals(subType)) return "尿";
        if ("dirty".equals(subType)) return "便";
        if ("both".equals(subType)) return "尿+便";
        if ("dry".equals(subType)) return "干";
        if ("fever".equals(subType)) return "发热";
        if ("diarrhea".equals(subType)) return "腹泻";
        if ("vomit".equals(subType)) return "呕吐";
        if ("medicine".equals(subType) || "other".equals(subType)) return "其他异常";
        return StringUtils.hasText(subType) ? subType : "未分类";
    }

    /**
     * 比较两条记录的时间，先按 recordTime 比较，相同时按 ID 比较
     * Compare two records by time; if equal, fall back to ID comparison
     */
    private int compareTime(ChildRecord left, ChildRecord right) {
        LocalDateTime leftTime = left.getRecordTime();
        LocalDateTime rightTime = right.getRecordTime();
        if (leftTime == null && rightTime == null) {
            return Long.compare(safeLong(left.getId()), safeLong(right.getId()));
        }
        if (leftTime == null) {
            return -1;
        }
        if (rightTime == null) {
            return 1;
        }
        int compared = leftTime.compareTo(rightTime);
        return compared != 0 ? compared : Long.compare(safeLong(left.getId()), safeLong(right.getId()));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将秒数转换为可读时长字符串（X小时X分钟 或 X分钟）
     * Convert seconds to human-readable duration string (X小时X分钟 or X分钟)
     */
    private String formatDuration(Integer seconds) {
        int safeSeconds = seconds == null ? 0 : Math.max(0, seconds);
        int minutes = (safeSeconds + 59) / 60;
        int hours = minutes / 60;
        int remainMinutes = minutes % 60;
        if (hours > 0) {
            return hours + "小时" + remainMinutes + "分钟";
        }
        return minutes + "分钟";
    }

    /**
     * 获取记录的时间字符串：优先使用 recordTime，其次使用 recordDate
     * Get the time string of a record: prefer recordTime, fallback to recordDate
     */
    private String formatDateTime(ChildRecord record) {
        if (record.getRecordTime() != null) {
            return record.getRecordTime().format(DATE_TIME);
        }
        return record.getRecordDate() == null ? "" : record.getRecordDate().toString();
    }

    /**
     * 将 LocalDateTime 格式化为 “yyyy-MM-dd HH:mm” 字符串，null 时返回 null
     * Format LocalDateTime to "yyyy-MM-dd HH:mm" string; return null if null
     */
    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME);
    }

    /**
     * 将字符串去除换行符并截断到指定最大长度，超长时添加 "..."
     * Remove newlines from string and truncate to max length, appending "..." if truncated
     */
    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private static class AnalysisComparison {

        private final AiAnalysisRecord previousAnalysis;

        private final AnalysisMetrics currentMetrics;

        private final AnalysisMetrics previousMetrics;

        private AnalysisComparison(AiAnalysisRecord previousAnalysis,
                                   AnalysisMetrics currentMetrics,
                                   AnalysisMetrics previousMetrics) {
            this.previousAnalysis = previousAnalysis;
            this.currentMetrics = currentMetrics;
            this.previousMetrics = previousMetrics;
        }
    }

    private static class AnalysisMetrics {

        private final LocalDate startDate;

        private final LocalDate endDate;

        private final int recordDays;

        private final int totalRecords;

        private final int feedCount;

        private final int bottleMilkMl;

        private final int breastDurationSec;

        private final int sleepCount;

        private final int sleepDurationSec;

        private final int diaperCount;

        private final int abnormalCount;

        private AnalysisMetrics(LocalDate startDate,
                                LocalDate endDate,
                                int recordDays,
                                int totalRecords,
                                int feedCount,
                                int bottleMilkMl,
                                int breastDurationSec,
                                int sleepCount,
                                int sleepDurationSec,
                                int diaperCount,
                                int abnormalCount) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.recordDays = recordDays;
            this.totalRecords = totalRecords;
            this.feedCount = feedCount;
            this.bottleMilkMl = bottleMilkMl;
            this.breastDurationSec = breastDurationSec;
            this.sleepCount = sleepCount;
            this.sleepDurationSec = sleepDurationSec;
            this.diaperCount = diaperCount;
            this.abnormalCount = abnormalCount;
        }

        private boolean hasData() {
            return totalRecords > 0;
        }
    }
}
