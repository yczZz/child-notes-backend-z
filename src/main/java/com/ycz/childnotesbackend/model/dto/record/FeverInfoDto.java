package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeverInfoDto extends BaseRecordDto {

    private BigDecimal temperature;

    private Boolean isAbnormal;

    private List<String> respiratory;

    private List<String> diarrhea;

    private String vomit;

    private String other;

    private MedicineDto medicine;

    private String note;

    private List<String> photos;

    private String time;
}
