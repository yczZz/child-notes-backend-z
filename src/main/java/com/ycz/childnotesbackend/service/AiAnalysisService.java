package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.ai.AiAnalysisRecordDto;

import java.util.List;

public interface AiAnalysisService {

    /**
     * 生成当前宝宝的AI分析报告
     * Generate AI analysis report for the current baby
     */
    AiAnalysisRecordDto generateCurrentBabyAnalysis();

    /**
     * 生成当前宝宝指定 7 天区间的 AI 分析报告
     * Generate AI analysis report for the current baby's selected 7-day range
     */
    AiAnalysisRecordDto generateCurrentBabyAnalysis(String startDate, String endDate);

    /**
     * 获取当前宝宝的AI分析历史列表
     * Get AI analysis history list for the current baby
     */
    List<AiAnalysisRecordDto> listCurrentBabyAnalyses();

    /**
     * 获取AI分析报告详情
     * Get AI analysis report detail
     *
     * @param id 分析记录ID / analysis record ID
     * @return AI分析报告详情 / AI analysis report detail
     */
    AiAnalysisRecordDto getAnalysisDetail(Long id);
}
