package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TodayStatsResponse {

    private String lastFeedTime;

    private String timeSinceLastFeed;

    private Integer feedCount;

    private Integer totalMilk;

    private String todaySleepTotal;

    private Integer totalSleepMin;

    private Integer sleepCount;

    private Integer diaperCount;

    private Integer wetDiaperCount;

    private Integer dirtyDiaperCount;

    private BigDecimal latestHeight;

    private BigDecimal latestWeight;

    private String latestGrowthTime;

    private Boolean hasFever;

    private FeverInfoDto feverInfo;

    private String lastMedicineTime;

    private Boolean hasDiarrhea;

    private String diarrheaTime;

    private String diarrheaTypes;

    private Boolean hasOtherAbnormal;

    private FeverInfoDto otherAbnormalInfo;

    private List<String> dailyTips;
}
