package com.ycz.childnotesbackend.model.dto.record;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomItemsResponse {

    private List<String> items = new ArrayList<>();
}
