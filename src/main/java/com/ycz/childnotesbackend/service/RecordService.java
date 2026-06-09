package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.record.AbnormalRecordDto;
import com.ycz.childnotesbackend.model.dto.record.ActivityRecordDto;
import com.ycz.childnotesbackend.model.dto.record.ComplementaryRecordDto;
import com.ycz.childnotesbackend.model.dto.record.CustomVaccineDto;
import com.ycz.childnotesbackend.model.dto.record.CustomItemsResponse;
import com.ycz.childnotesbackend.model.dto.record.DailyRecordsResponse;
import com.ycz.childnotesbackend.model.dto.record.DailyStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.DiaperRecordDto;
import com.ycz.childnotesbackend.model.dto.record.FeedRecordDto;
import com.ycz.childnotesbackend.model.dto.record.GrowthRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MaternalFoodRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MilestoneRecordDto;
import com.ycz.childnotesbackend.model.dto.record.PumpRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SleepRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SupplementOptionsResponse;
import com.ycz.childnotesbackend.model.dto.record.SupplementRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TemperatureRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TodayStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.VaccineRecordDto;

import java.time.LocalDate;
import java.util.List;

public interface RecordService {

    /**
     * 获取今日记录
     * Get today's records
     *
     * @return 今日记录 / today's records
     */
    DailyRecordsResponse getTodayRecords();

    /**
     * 获取今日统计
     * Get today's statistics
     *
     * @return 今日统计 / today's statistics
     */
    TodayStatsResponse getTodayStats();

    /**
     * 获取指定日期的统计
     * Get daily statistics for specified date
     *
     * @param date 日期 / date
     * @return 日期统计 / daily statistics
     */
    DailyStatsResponse getDailyStats(LocalDate date);

    /**
     * 获取日期范围内的统计
     * Get daily statistics for a date range
     *
     * @param startDate 开始日期 / start date
     * @param endDate   结束日期 / end date
     * @return 统计列表 / statistics list
     */
    List<DailyStatsResponse> getDailyStatsRange(LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定日期的记录
     * Get records by date
     *
     * @param date 日期 / date
     * @return 日期记录 / records for the date
     */
    DailyRecordsResponse getRecordsByDate(LocalDate date);

    /**
     * 获取历史记录
     * Get history records
     *
     * @return 历史记录列表 / history records list
     */
    List<DailyRecordsResponse> getHistoryRecords();

    /**
     * 添加喂养记录
     * Add feed record
     *
     * @param data 喂养记录数据 / feed record data
     * @return 添加后的喂养记录 / added feed record
     */
    FeedRecordDto addFeedRecord(FeedRecordDto data);

    /**
     * 获取最新喂养记录
     * Get latest feed record
     *
     * @return 最新喂养记录 / latest feed record
     */
    FeedRecordDto getLatestFeed();

    /**
     * 添加换尿布记录
     * Add diaper record
     *
     * @param data 换尿布记录数据 / diaper record data
     * @return 添加后的换尿布记录 / added diaper record
     */
    DiaperRecordDto addDiaperRecord(DiaperRecordDto data);

    /**
     * 添加睡眠记录
     * Add sleep record
     *
     * @param data 睡眠记录数据 / sleep record data
     * @return 添加后的睡眠记录 / added sleep record
     */
    SleepRecordDto addSleepRecord(SleepRecordDto data);

    /**
     * 添加体温记录
     * Add temperature record
     *
     * @param data 体温记录数据 / temperature record data
     * @return 添加后的体温记录 / added temperature record
     */
    TemperatureRecordDto addTemperatureRecord(TemperatureRecordDto data);

    /**
     * 添加补充剂记录
     * Add supplement record
     *
     * @param data 补充剂记录数据 / supplement record data
     * @return 添加后的补充剂记录 / added supplement record
     */
    SupplementRecordDto addSupplementRecord(SupplementRecordDto data);

    /**
     * 获取补充剂选项
     * Get supplement options
     *
     * @return 补充剂选项 / supplement options
     */
    SupplementOptionsResponse getSupplementOptions();

    /**
     * 添加生长记录
     * Add growth record
     *
     * @param data 生长记录数据 / growth record data
     * @return 添加后的生长记录 / added growth record
     */
    GrowthRecordDto addGrowthRecord(GrowthRecordDto data);

    /**
     * 添加异常记录
     * Add abnormal record
     *
     * @param data 异常记录数据 / abnormal record data
     * @return 添加后的异常记录 / added abnormal record
     */
    AbnormalRecordDto addAbnormalRecord(AbnormalRecordDto data);

    /**
     * 添加吸奶记录
     * Add pump record
     *
     * @param data 吸奶记录数据 / pump record data
     * @return 添加后的吸奶记录 / added pump record
     */
    PumpRecordDto addPumpRecord(PumpRecordDto data);

    /**
     * 添加辅食记录
     * Add complementary food record
     *
     * @param data 辅食记录数据 / complementary record data
     * @return 添加后的辅食记录 / added complementary record
     */
    ComplementaryRecordDto addComplementaryRecord(ComplementaryRecordDto data);

    /**
     * Add maternal diet observation record.
     */
    MaternalFoodRecordDto addMaternalFoodRecord(MaternalFoodRecordDto data);

    /**
     * 标记发烧已恢复
     * Mark fever as resolved
     */
    void markFeverResolved();

    /**
     * 标记腹泻已恢复
     * Mark diarrhea as resolved
     */
    void markDiarrheaResolved();

    /**
     * 标记其他异常已恢复
     * Mark other abnormal symptoms as resolved
     */
    void markAbnormalResolved();

    /**
     * 添加疫苗记录
     * Add vaccine record
     *
     * @param data 疫苗记录数据 / vaccine record data
     * @return 添加后的疫苗记录 / added vaccine record
     */
    VaccineRecordDto addVaccineRecord(VaccineRecordDto data);

    /**
     * 获取疫苗列表
     * Get vaccine list
     *
     * @return 疫苗列表 / vaccine list
     */
    List<VaccineRecordDto> getVaccines();

    /**
     * 获取当前用户当前宝宝的自定义疫苗
     * Get custom vaccines for current user and baby
     *
     * @return 自定义疫苗列表 / custom vaccine list
     */
    List<CustomVaccineDto> getCustomVaccines();

    /**
     * 添加当前用户当前宝宝的自定义疫苗
     * Add a custom vaccine for current user and baby
     *
     * @param data 自定义疫苗数据 / custom vaccine data
     * @return 保存后的自定义疫苗 / saved custom vaccine
     */
    CustomVaccineDto addCustomVaccine(CustomVaccineDto data);

    /**
     * 添加活动记录
     * Add activity record
     *
     * @param data 活动记录数据 / activity record data
     * @return 添加后的活动记录 / added activity record
     */
    ActivityRecordDto addActivityRecord(ActivityRecordDto data);

    /**
     * 获取活动列表
     * Get activity list
     *
     * @return 活动列表 / activity list
     */
    List<ActivityRecordDto> getActivities();

    /**
     * 结束睡眠
     * Wake up from sleep
     *
     * @param sleepId 睡眠记录ID / sleep record ID
     * @return 更新后的睡眠记录 / updated sleep record
     */
    SleepRecordDto wakeUpSleep(Long sleepId);

    /**
     * 获取最新睡眠记录
     * Get latest sleep record
     *
     * @return 最新睡眠记录 / latest sleep record
     */
    SleepRecordDto getLatestSleep();

    /**
     * 更新记录
     * Update record
     *
     * @param id         记录ID / record ID
     * @param payloadJson 记录JSON数据 / record JSON data
     */
    void updateRecord(Long id, String payloadJson);

    /**
     * 逻辑删除记录
     * Logically delete a record
     *
     * @param id 记录ID / record ID
     */
    void deleteRecord(Long id);

    /**
     * 添加里程碑
     * Add milestone
     *
     * @param data 里程碑数据 / milestone data
     * @return 添加后的里程碑 / added milestone
     */
    MilestoneRecordDto addMilestone(MilestoneRecordDto data);

    /**
     * 获取里程碑列表
     * Get milestone list
     *
     * @return 里程碑列表 / milestone list
     */
    List<MilestoneRecordDto> getMilestones();

    /**
     * 更新里程碑
     * Update milestone
     *
     * @param id   里程碑ID / milestone ID
     * @param data 里程碑数据 / milestone data
     * @return 更新后的里程碑 / updated milestone
     */
    MilestoneRecordDto updateMilestone(Long id, MilestoneRecordDto data);

    /**
     * 获取自定义项目
     * Get custom items
     *
     * @param type 项目类型 / item type
     * @return 自定义项目响应 / custom items response
     */
    CustomItemsResponse getCustomItems(String type);

    /**
     * 删除自定义项目
     * Delete a custom item
     *
     * @param type 项目类型 / item type
     * @param name 项目名称 / item name
     */
    void deleteCustomItem(String type, String name);
}
