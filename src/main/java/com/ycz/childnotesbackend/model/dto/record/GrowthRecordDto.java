package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrowthRecordDto extends BaseRecordDto {

    private BigDecimal height;

    private BigDecimal weight;

    private String note;

    private String time;
}
