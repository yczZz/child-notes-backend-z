package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeedRecordDto extends BaseRecordDto {

    private String type;

    private String side;

    private Integer duration;

    private Integer leftDuration;

    private Integer leftDurationSec;

    private String leftStartTime;

    private Integer rightDuration;

    private Integer rightDurationSec;

    private String rightStartTime;

    private Integer amount;

    private String note;

    private String time;

    private Boolean saveProgress;
}
