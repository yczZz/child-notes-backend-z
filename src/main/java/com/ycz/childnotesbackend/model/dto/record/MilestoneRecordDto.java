package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MilestoneRecordDto extends BaseRecordDto {

    private String title;

    private String content;

    private String date;

    private List<String> photos;
}
