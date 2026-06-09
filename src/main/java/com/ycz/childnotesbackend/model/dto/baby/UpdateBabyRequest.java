package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

@Data
public class UpdateBabyRequest {

    private Long id;

    private String name;

    private String avatar;

    private String gender;

    private String birthDate;
}
