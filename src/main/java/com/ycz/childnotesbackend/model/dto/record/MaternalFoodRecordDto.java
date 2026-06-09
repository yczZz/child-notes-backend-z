package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MaternalFoodRecordDto extends BaseRecordDto {

    private String mealType;

    private List<String> foods;

    private List<String> customFoods;

    private String suspicionLevel;

    private String note;

    private List<String> photos;

    private String time;
}
