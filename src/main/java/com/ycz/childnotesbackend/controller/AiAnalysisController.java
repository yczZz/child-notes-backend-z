package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.ai.AiAnalysisRecordDto;
import com.ycz.childnotesbackend.model.dto.ai.GenerateAiAnalysisRequest;
import com.ycz.childnotesbackend.service.AiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/smart-analysis")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/generate")
    public Response<AiAnalysisRecordDto> generateCurrentBabyAnalysis(@RequestBody(required = false) GenerateAiAnalysisRequest request) {
        String startDate = request == null ? null : request.getStartDate();
        String endDate = request == null ? null : request.getEndDate();
        return new Response<>(aiAnalysisService.generateCurrentBabyAnalysis(startDate, endDate));
    }

    @GetMapping("/list")
    public Response<List<AiAnalysisRecordDto>> listCurrentBabyAnalyses() {
        return new Response<>(aiAnalysisService.listCurrentBabyAnalyses());
    }

    @GetMapping("/{id}")
    public Response<AiAnalysisRecordDto> getAnalysisDetail(@PathVariable Long id) {
        return new Response<>(aiAnalysisService.getAnalysisDetail(id));
    }
}
