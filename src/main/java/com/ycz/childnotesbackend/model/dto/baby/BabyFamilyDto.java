package com.ycz.childnotesbackend.model.dto.baby;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BabyFamilyDto {

    private Long babyId;

    private String babyName;

    private String babyAvatar;

    private String babyGender;

    private String birthDate;

    private List<BabyMemberDto> members = new ArrayList<>();
}
