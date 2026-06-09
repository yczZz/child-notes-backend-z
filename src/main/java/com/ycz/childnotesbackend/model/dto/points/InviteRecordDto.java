package com.ycz.childnotesbackend.model.dto.points;

import lombok.Data;

@Data
public class InviteRecordDto {

    private Long id;

    private Long invitedUserId;

    private String invitedNickName;

    private String invitedAvatarUrl;

    private Integer points;

    private String createdAt;
}
