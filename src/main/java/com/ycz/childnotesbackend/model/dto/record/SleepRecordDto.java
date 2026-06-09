package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SleepRecordDto extends BaseRecordDto {

    private String startTime;

    private String endTime;

    private String displayStartTime;

    private String displayEndTime;

    private Integer duration;

    private String note;
}
