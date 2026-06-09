package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PumpRecordDto extends BaseRecordDto {

    private Integer leftDuration;

    private Integer rightDuration;

    private Integer leftAmount;

    private Integer rightAmount;

    private Integer totalAmount;

    private String note;

    private String time;
}
