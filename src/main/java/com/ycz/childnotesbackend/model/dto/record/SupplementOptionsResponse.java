package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SupplementOptionsResponse {

    private List<String> supplements = new ArrayList<>();

    private List<String> medicines = new ArrayList<>();

    private List<String> doseUnits = new ArrayList<>();
}
