package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class JoinFamilyRequest {

    private Long babyId;

    private String roleCode;

    private String roleName;
}
