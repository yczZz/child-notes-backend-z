package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminPageResponse<T> {

    private long total;

    private int page;

    private int pageSize;

    private List<T> records = new ArrayList<>();
}
