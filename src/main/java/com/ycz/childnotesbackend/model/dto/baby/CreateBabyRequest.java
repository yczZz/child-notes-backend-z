package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class CreateBabyRequest {

    private String name;

    private String avatar;

    private String gender;

    private String birthDate;

    private String roleCode;

    private String roleName;
}
