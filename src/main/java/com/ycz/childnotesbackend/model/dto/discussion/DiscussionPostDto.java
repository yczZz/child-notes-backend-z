package com.ycz.childnotesbackend.model.dto.discussion;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DiscussionPostDto {

    private Long id;

    private String title;

    private String content;

    private String category;

    private String categoryName;

    private List<String> images = new ArrayList<>();

    private Integer commentCount;

    private Long authorId;

    private String authorName;

    private String authorAvatarUrl;

    private Boolean mine;

    private String createdAt;

    private String updatedAt;
}
