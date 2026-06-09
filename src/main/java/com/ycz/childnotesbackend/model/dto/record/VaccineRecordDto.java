package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VaccineRecordDto extends BaseRecordDto {

    private String name;

    private String vaccineId;

    private String doseId;

    private String category;

    private String doseLabel;

    private Boolean custom;

    private String recommendedDate;

    private String status;

    private Boolean skipped;

    private String skippedReason;

    private String nextName;

    private String nextDate;

    private String note;

    private String time;
}
