package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.util.Map;

@Data
public class CustomVaccineDto {

    private Long id;

    private Long babyId;

    private String name;

    private String category;

    private String disease;

    private String doseLabel;

    private String ageLabel;

    private Map<String, Integer> due;

    private Boolean custom;
}
