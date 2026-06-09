package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyStatsResponse {

    private String date;

    private Integer recordCount = 0;

    private FeedStats feed = new FeedStats();

    private SleepStats sleep = new SleepStats();

    private DiaperStats diaper = new DiaperStats();

    private TemperatureStats temperature = new TemperatureStats();

    private SupplementStats supplement = new SupplementStats();

    private GrowthStats growth = new GrowthStats();

    private PumpStats pump = new PumpStats();

    private ComplementaryStats complementary = new ComplementaryStats();

    private MaternalFoodStats maternalFood = new MaternalFoodStats();

    private AbnormalStats abnormal = new AbnormalStats();

    private VaccineStats vaccine = new VaccineStats();

    private ActivityStats activity = new ActivityStats();

    private MilestoneStats milestone = new MilestoneStats();

    @Data
    public static class FeedStats {
        private Integer count = 0;
        private Integer breastCount = 0;
        private Integer bottleCount = 0;
        private Integer expressedCount = 0;
        private Integer totalMilk = 0;
        private Integer bottleMilk = 0;
        private Integer expressedMilk = 0;
        private Integer breastDurationSec = 0;
        private Integer breastLeftDurationSec = 0;
        private Integer breastRightDurationSec = 0;
        private String lastFeedTime;
    }

    @Data
    public static class SleepStats {
        private Integer count = 0;
        private Integer ongoingCount = 0;
        private Integer totalDurationMin = 0;
        private Integer totalDurationSec = 0;
    }

    @Data
    public static class DiaperStats {
        private Integer count = 0;
        private Integer wetCount = 0;
        private Integer dirtyCount = 0;
        private Integer bothCount = 0;
        private Integer dryCount = 0;
        private Integer abnormalCount = 0;
        private Integer diarrheaCount = 0;
    }

    @Data
    public static class TemperatureStats {
        private Integer count = 0;
        private Integer abnormalCount = 0;
        private BigDecimal maxTemperature;
        private String maxTemperatureTime;
    }

    @Data
    public static class SupplementStats {
        private Integer count = 0;
        private Integer medicineCount = 0;
        private Integer nutritionCount = 0;
    }

    @Data
    public static class GrowthStats {
        private Integer count = 0;
        private BigDecimal latestHeight;
        private BigDecimal latestWeight;
        private String latestTime;
    }

    @Data
    public static class PumpStats {
        private Integer count = 0;
        private Integer totalAmount = 0;
        private Integer totalDurationMin = 0;
        private Integer totalDurationSec = 0;
    }

    @Data
    public static class ComplementaryStats {
        private Integer count = 0;
        private Integer abnormalCount = 0;
    }

    @Data
    public static class MaternalFoodStats {
        private Integer count = 0;
        private Integer suspectCount = 0;
    }

    @Data
    public static class AbnormalStats {
        private Integer count = 0;
        private Integer feverCount = 0;
        private Integer diarrheaCount = 0;
        private Integer vomitCount = 0;
        private Integer medicineCount = 0;
        private Integer otherCount = 0;
    }

    @Data
    public static class VaccineStats {
        private Integer count = 0;
    }

    @Data
    public static class ActivityStats {
        private Integer count = 0;
        private Integer totalDurationMin = 0;
        private Integer totalDurationSec = 0;
    }

    @Data
    public static class MilestoneStats {
        private Integer count = 0;
    }
}
