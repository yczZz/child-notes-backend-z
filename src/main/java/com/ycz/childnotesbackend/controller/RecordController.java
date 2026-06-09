package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.record.AbnormalRecordDto;
import com.ycz.childnotesbackend.model.dto.record.DailyRecordsResponse;
import com.ycz.childnotesbackend.model.dto.record.DailyStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.DiaperRecordDto;
import com.ycz.childnotesbackend.model.dto.record.FeedRecordDto;
import com.ycz.childnotesbackend.model.dto.record.GrowthRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MaternalFoodRecordDto;
import com.ycz.childnotesbackend.model.dto.record.MilestoneRecordDto;
import com.ycz.childnotesbackend.model.dto.record.ComplementaryRecordDto;
import com.ycz.childnotesbackend.model.dto.record.CustomItemsResponse;
import com.ycz.childnotesbackend.model.dto.record.CustomVaccineDto;
import com.ycz.childnotesbackend.model.dto.record.PumpRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SleepRecordDto;
import com.ycz.childnotesbackend.model.dto.record.SupplementOptionsResponse;
import com.ycz.childnotesbackend.model.dto.record.SupplementRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TemperatureRecordDto;
import com.ycz.childnotesbackend.model.dto.record.TodayStatsResponse;
import com.ycz.childnotesbackend.model.dto.record.ActivityRecordDto;
import com.ycz.childnotesbackend.model.dto.record.VaccineRecordDto;
import com.ycz.childnotesbackend.service.RecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/today")
    public Response<DailyRecordsResponse> getTodayRecords() {
        return new Response<>(recordService.getTodayRecords());
    }

    @GetMapping("/today/stats")
    public Response<TodayStatsResponse> getTodayStats() {
        return new Response<>(recordService.getTodayStats());
    }

    @GetMapping("/stats/date")
    public Response<DailyStatsResponse> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new Response<>(recordService.getDailyStats(date));
    }

    @GetMapping("/stats/range")
    public Response<List<DailyStatsResponse>> getDailyStatsRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return new Response<>(recordService.getDailyStatsRange(startDate, endDate));
    }

    @GetMapping("/date")
    public Response<DailyRecordsResponse> getRecordsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new Response<>(recordService.getRecordsByDate(date));
    }

    @GetMapping("/history")
    public Response<List<DailyRecordsResponse>> getHistoryRecords() {
        return new Response<>(recordService.getHistoryRecords());
    }

    @GetMapping("/feed/latest")
    public Response<FeedRecordDto> getLatestFeed() {
        return new Response<>(recordService.getLatestFeed());
    }

    @PostMapping("/feed")
    public Response<FeedRecordDto> addFeedRecord(@RequestBody FeedRecordDto data) {
        return new Response<>(recordService.addFeedRecord(data));
    }

    @PostMapping("/diaper")
    public Response<DiaperRecordDto> addDiaperRecord(@RequestBody DiaperRecordDto data) {
        return new Response<>(recordService.addDiaperRecord(data));
    }

    @PostMapping("/sleep")
    public Response<SleepRecordDto> addSleepRecord(@RequestBody SleepRecordDto data) {
        return new Response<>(recordService.addSleepRecord(data));
    }

    @PostMapping("/temperature")
    public Response<TemperatureRecordDto> addTemperatureRecord(@RequestBody TemperatureRecordDto data) {
        return new Response<>(recordService.addTemperatureRecord(data));
    }

    @PostMapping("/supplement")
    public Response<SupplementRecordDto> addSupplementRecord(@RequestBody SupplementRecordDto data) {
        return new Response<>(recordService.addSupplementRecord(data));
    }

    @GetMapping("/supplement/custom-items")
    public Response<SupplementOptionsResponse> getSupplementOptions() {
        return new Response<>(recordService.getSupplementOptions());
    }

    @PostMapping("/growth")
    public Response<GrowthRecordDto> addGrowthRecord(@RequestBody GrowthRecordDto data) {
        return new Response<>(recordService.addGrowthRecord(data));
    }

    @PostMapping("/abnormal")
    public Response<AbnormalRecordDto> addAbnormalRecord(@RequestBody AbnormalRecordDto data) {
        return new Response<>(recordService.addAbnormalRecord(data));
    }

    @PostMapping("/pump")
    public Response<PumpRecordDto> addPumpRecord(@RequestBody PumpRecordDto data) {
        return new Response<>(recordService.addPumpRecord(data));
    }

    @PostMapping("/complementary")
    public Response<ComplementaryRecordDto> addComplementaryRecord(@RequestBody ComplementaryRecordDto data) {
        return new Response<>(recordService.addComplementaryRecord(data));
    }

    @PostMapping("/maternal-food")
    public Response<MaternalFoodRecordDto> addMaternalFoodRecord(@RequestBody MaternalFoodRecordDto data) {
        return new Response<>(recordService.addMaternalFoodRecord(data));
    }

    @PostMapping("/fever-resolved")
    public Response<Void> markFeverResolved() {
        recordService.markFeverResolved();
        return Response.SUCCESS;
    }

    @PostMapping("/diarrhea-resolved")
    public Response<Void> markDiarrheaResolved() {
        recordService.markDiarrheaResolved();
        return Response.SUCCESS;
    }

    @PostMapping("/abnormal-resolved")
    public Response<Void> markAbnormalResolved() {
        recordService.markAbnormalResolved();
        return Response.SUCCESS;
    }

    @PostMapping("/vaccine")
    public Response<VaccineRecordDto> addVaccineRecord(@RequestBody VaccineRecordDto data) {
        return new Response<>(recordService.addVaccineRecord(data));
    }

    @GetMapping("/vaccines")
    public Response<List<VaccineRecordDto>> getVaccines() {
        return new Response<>(recordService.getVaccines());
    }

    @GetMapping("/vaccines/custom")
    public Response<List<CustomVaccineDto>> getCustomVaccines() {
        return new Response<>(recordService.getCustomVaccines());
    }

    @PostMapping("/vaccines/custom")
    public Response<CustomVaccineDto> addCustomVaccine(@RequestBody CustomVaccineDto data) {
        return new Response<>(recordService.addCustomVaccine(data));
    }

    @PostMapping("/activity")
    public Response<ActivityRecordDto> addActivityRecord(@RequestBody ActivityRecordDto data) {
        return new Response<>(recordService.addActivityRecord(data));
    }

    @GetMapping("/activities")
    public Response<List<ActivityRecordDto>> getActivities() {
        return new Response<>(recordService.getActivities());
    }

    @GetMapping("/sleep/latest")
    public Response<SleepRecordDto> getLatestSleep() {
        return new Response<>(recordService.getLatestSleep());
    }

    @PutMapping("/sleep/{sleepId}/wake")
    public Response<SleepRecordDto> wakeUpSleep(@PathVariable Long sleepId) {
        return new Response<>(recordService.wakeUpSleep(sleepId));
    }

    @PostMapping("/milestone")
    public Response<MilestoneRecordDto> addMilestone(@RequestBody MilestoneRecordDto data) {
        return new Response<>(recordService.addMilestone(data));
    }

    @GetMapping("/milestones")
    public Response<List<MilestoneRecordDto>> getMilestones() {
        return new Response<>(recordService.getMilestones());
    }

    @PutMapping("/milestone/{id}")
    public Response<MilestoneRecordDto> updateMilestone(@PathVariable Long id, @RequestBody MilestoneRecordDto data) {
        return new Response<>(recordService.updateMilestone(id, data));
    }

    @GetMapping("/custom-items")
    public Response<CustomItemsResponse> getCustomItems(@RequestParam String type) {
        return new Response<>(recordService.getCustomItems(type));
    }

    @DeleteMapping("/custom-items")
    public Response<Void> deleteCustomItem(@RequestParam String type, @RequestParam String name) {
        recordService.deleteCustomItem(type, name);
        return Response.SUCCESS;
    }

    @PutMapping("/{id}")
    public Response<Void> updateRecord(@PathVariable Long id, @RequestBody String payloadJson) {
        recordService.updateRecord(id, payloadJson);
        return Response.SUCCESS;
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return Response.SUCCESS;
    }
}
