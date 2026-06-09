package com.ycz.childnotesbackend.model.dto.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemperatureRecordDto extends BaseRecordDto {

    private BigDecimal temperature;

    private Boolean isAbnormal;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean resolved;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String resolvedTime;

    private String note;

    private String time;
}
