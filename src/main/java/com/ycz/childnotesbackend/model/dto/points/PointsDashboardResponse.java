package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PointsDashboardResponse {

    private Long points = 0L;

    private Long totalEarned = 0L;

    private Long totalSpent = 0L;

    private String shareReferrerId;

    private SignInSummaryDto signIn;

    private LotterySummaryDto lottery;

    private List<TaskTemplateDto> tasks = new ArrayList<>();

    private List<InviteRecordDto> inviteRecords = new ArrayList<>();
}
