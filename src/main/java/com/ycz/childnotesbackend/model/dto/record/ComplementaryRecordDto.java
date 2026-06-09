package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ComplementaryRecordDto extends BaseRecordDto {

    private List<String> foodTypes;

    private String texture;

    private String foodName;

    private String amount;

    private String amountUnit;

    private String note;

    private List<String> photos;

    private String reaction;

    private Boolean abnormal;

    private String time;

    private List<String> customFoodTypes;
}
