package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityRecordDto extends BaseRecordDto {

    private String name;

    private String category;

    private Integer duration;

    private String note;

    private String time;

    private Boolean customName;
}
