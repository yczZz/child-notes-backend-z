package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

@Data
public class TaskTemplateDto {

    private String taskKey;

    private String title;

    private String description;

    private Integer points;

    private String action;
}
