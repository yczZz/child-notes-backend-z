package com.ycz.childnotesbackend.model.dto.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbnormalRecordDto extends BaseRecordDto {

    private BigDecimal temperature;

    private List<String> respiratory;

    private List<String> diarrhea;

    private String vomit;

    private String other;

    private MedicineDto medicine;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean resolved;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String resolvedTime;

    private String note;

    private List<String> photos;

    private String time;
}
