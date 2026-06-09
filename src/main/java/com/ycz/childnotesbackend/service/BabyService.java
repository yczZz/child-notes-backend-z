package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.baby.BabyInfoDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyFamilyDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyGrowthStageDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyMemberDto;
import com.ycz.childnotesbackend.model.dto.baby.CreateBabyRequest;
import com.ycz.childnotesbackend.model.dto.baby.JoinFamilyRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyMemberRoleRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyRequest;

import java.util.List;

public interface BabyService {

    /**
     * 获取当前宝宝信息
     * Get current baby information
     *
     * @return 当前宝宝信息 / current baby information
     */
    BabyInfoDto getCurrentBaby();

    /**
     * 获取宝宝列表
     * Get list of babies
     *
     * @return 宝宝列表 / list of babies
     */
    List<BabyInfoDto> listBabies();

    /**
     * 创建宝宝
     * Create a new baby
     *
     * @param request 创建宝宝请求参数 / create baby request parameters
     * @return 创建后的宝宝信息 / created baby information
     */
    BabyInfoDto createBaby(CreateBabyRequest request);

    /**
     * 更新宝宝信息
     * Update baby information
     *
     * @param request 更新宝宝请求参数 / update baby request parameters
     * @return 更新后的宝宝信息 / updated baby information
     */
    BabyInfoDto updateBaby(UpdateBabyRequest request);

    /**
     * 获取家庭成员列表
     * Get family members list
     *
     * @return 家庭成员列表 / list of family members
     */
    List<BabyFamilyDto> listFamilyMembers();

    /**
     * 更新当前用户在家庭中的角色
     * Update current user's role in the family
     *
     * @param request 更新角色请求参数 / update role request parameters
     * @return 更新后的成员信息 / updated member information
     */
    BabyMemberDto updateMyFamilyRole(UpdateBabyMemberRoleRequest request);

    /**
     * 通过邀请码加入家庭
     * Join family via invitation code
     *
     * @param request 加入家庭请求参数 / join family request parameters
     * @return 加入后的成员信息 / member information after joining
     */
    BabyMemberDto joinFamilyViaInvite(JoinFamilyRequest request);

    /**
     * 获取宝宝成长阶段信息
     * Get baby growth stage information
     *
     * @param babyId 宝宝ID / baby ID
     * @return 成长阶段信息 / growth stage information
     */
    BabyGrowthStageDto getGrowthStage(Long babyId);
}
