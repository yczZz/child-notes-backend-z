package com.ycz.childnotesbackend.model.dto.discussion;

import lombok.Data;

@Data
public class DiscussionCommentDto {

    private Long id;

    private Long postId;

    private String content;

    private Long authorId;

    private String authorName;

    private String authorAvatarUrl;

    private Boolean mine;

    private String createdAt;

    private String updatedAt;
}
