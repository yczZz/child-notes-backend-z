package com.ycz.childnotesbackend.model.dto.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiaperRecordDto extends BaseRecordDto {

    private String type;

    private String color;

    private String urineColor;

    private String urineAmount;

    private String consistency;

    private String stoolAmount;

    private List<String> diarrhea;

    private Boolean abnormal;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean resolved;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String resolvedTime;

    private List<String> photos;

    private String note;

    private String time;
}
