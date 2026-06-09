package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.baby.BabyFamilyDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyGrowthStageDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyInfoDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyMemberDto;
import com.ycz.childnotesbackend.model.dto.baby.CreateBabyRequest;
import com.ycz.childnotesbackend.model.dto.baby.JoinFamilyRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyMemberRoleRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyRequest;
import com.ycz.childnotesbackend.service.BabyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/baby")
public class BabyController {

    private final BabyService babyService;

    public BabyController(BabyService babyService) {
        this.babyService = babyService;
    }

    @GetMapping("/current")
    public Response<BabyInfoDto> getCurrentBaby() {
        return new Response<>(babyService.getCurrentBaby());
    }

    @GetMapping("/list")
    public Response<List<BabyInfoDto>> listBabies() {
        return new Response<>(babyService.listBabies());
    }

    @PostMapping("/add")
    public Response<BabyInfoDto> createBaby(@RequestBody CreateBabyRequest request) {
        return new Response<>(babyService.createBaby(request));
    }

    @PutMapping("/update")
    public Response<BabyInfoDto> updateBaby(@RequestBody UpdateBabyRequest request) {
        return new Response<>(babyService.updateBaby(request));
    }

    @GetMapping("/family/members")
    public Response<List<BabyFamilyDto>> listFamilyMembers() {
        return new Response<>(babyService.listFamilyMembers());
    }

    @PutMapping("/family/my-role")
    public Response<BabyMemberDto> updateMyFamilyRole(@RequestBody UpdateBabyMemberRoleRequest request) {
        return new Response<>(babyService.updateMyFamilyRole(request));
    }

    @PostMapping("/family/join")
    public Response<BabyMemberDto> joinFamilyViaInvite(@RequestBody JoinFamilyRequest request) {
        return new Response<>(babyService.joinFamilyViaInvite(request));
    }

    @GetMapping("/growth-stage")
    public Response<BabyGrowthStageDto> getGrowthStage(@RequestParam(required = false) Long babyId) {
        return new Response<>(babyService.getGrowthStage(babyId));
    }
}
