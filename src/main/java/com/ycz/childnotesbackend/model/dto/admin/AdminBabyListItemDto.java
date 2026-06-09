package com.ycz.childnotesbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminBabyListItemDto {

    private Long id;

    private String name;

    private String avatar;

    private String gender;

    private String birthDate;

    private Long ageDays;

    private Long ownerUserId;

    private String ownerNickName;

    private Long memberCount;

    private String createdAt;
}
