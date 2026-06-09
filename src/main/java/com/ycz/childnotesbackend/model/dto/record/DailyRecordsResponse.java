package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DailyRecordsResponse {

    private String date;

    private List<FeedRecordDto> feeds = new ArrayList<>();

    private List<DiaperRecordDto> diapers = new ArrayList<>();

    private List<SleepRecordDto> sleeps = new ArrayList<>();

    private List<SupplementRecordDto> supplements = new ArrayList<>();

    private List<GrowthRecordDto> growths = new ArrayList<>();

    private List<TemperatureRecordDto> temperatures = new ArrayList<>();

    private List<AbnormalRecordDto> abnormals = new ArrayList<>();

    private List<PumpRecordDto> pumps = new ArrayList<>();

    private List<ComplementaryRecordDto> complementaries = new ArrayList<>();

    private List<MaternalFoodRecordDto> maternalFoods = new ArrayList<>();
}
