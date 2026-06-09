package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class UpdateBabyMemberRoleRequest {

    private Long babyId;

    private String roleCode;

    private String roleName;
}
