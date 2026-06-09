package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupplementRecordDto extends BaseRecordDto {

    private String type;

    private String name;

    private String dose;

    private String doseUnit;

    private String note;

    private String time;

    private Boolean customName;

    private Boolean customDoseUnit;
}
