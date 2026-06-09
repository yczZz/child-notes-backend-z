package com.ycz.childnotesbackend.model.dto.discussion;

import lombok.Data;

import java.util.List;

@Data
public class CreateDiscussionPostRequest {

    private String title;

    private String content;

    private String category;

    private List<String> images;
}
