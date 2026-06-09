package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.mapper.BabyGrowthStageMapper;
import com.ycz.childnotesbackend.mapper.BabyMemberMapper;
import com.ycz.childnotesbackend.mapper.BabyMapper;
import com.ycz.childnotesbackend.model.dto.baby.BabyFamilyDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyGrowthStageDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyInfoDto;
import com.ycz.childnotesbackend.model.dto.baby.BabyMemberDto;
import com.ycz.childnotesbackend.model.dto.baby.CreateBabyRequest;
import com.ycz.childnotesbackend.model.dto.baby.JoinFamilyRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyMemberRoleRequest;
import com.ycz.childnotesbackend.model.dto.baby.UpdateBabyRequest;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.model.entity.Baby;
import com.ycz.childnotesbackend.model.entity.BabyGrowthStage;
import com.ycz.childnotesbackend.model.entity.BabyMember;
import com.ycz.childnotesbackend.service.BabyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BabyServiceImpl implements BabyService {

    private static final String ACTIVE_STATUS = "active";

    private static final String DEFAULT_ROLE_CODE = "guardian";

    private static final String DEFAULT_ROLE_NAME = "家人";

    private final BabyMapper babyMapper;

    private final BabyMemberMapper babyMemberMapper;

    private final AppUserMapper appUserMapper;

    private final BabyGrowthStageMapper babyGrowthStageMapper;

    public BabyServiceImpl(BabyMapper babyMapper, BabyMemberMapper babyMemberMapper,
                           AppUserMapper appUserMapper, BabyGrowthStageMapper babyGrowthStageMapper) {
        this.babyMapper = babyMapper;
        this.babyMemberMapper = babyMemberMapper;
        this.appUserMapper = appUserMapper;
        this.babyGrowthStageMapper = babyGrowthStageMapper;
    }

    /**
     * 获取当前用户的当前宝宝信息
     * Get current baby information for the current user
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前登录用户ID
     *    Get current logged-in user ID
     * 2. 从请求头 X-Baby-Id 或查询参数中获取指定的 babyId
     *    Read requested babyId from header X-Baby-Id or query parameter
     * 3. 若指定了 babyId 则按权限查找并验证用户有访问权；否则取用户第一个宝宝
     *    If babyId specified, find baby and verify user access; otherwise find user's first baby
     * 4. 如果没有宝宝返回 null，否则组装 DTO 返回
     *    Return null if no baby found, otherwise assemble and return DTO
     *
     * @return 宝宝信息 DTO，如果用户无宝宝则返回 null / baby info DTO, null if user has no baby
     */
    @Override
    public BabyInfoDto getCurrentBaby() {
        Long userId = AuthContext.requireCurrentUserId();
        Long requestedBabyId = getRequestedBabyId();
        Baby baby = requestedBabyId == null
                ? findFirstBaby(userId)
                : findBabyForUser(requestedBabyId, userId);
        if (baby == null) {
            return null;
        }
        return toDto(baby, findMember(userId, baby.getId()));
    }

    /**
     * 获取当前用户所有可访问的宝宝列表
     * Get list of all babies accessible to the current user
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 查询用户已加入家庭的所有活跃成员记录，建立 babyId -> BabyMember 映射
     *    Query user's active family memberships, build babyId -> BabyMember map
     * 2. 合并用户自己创建的宝宝（可能尚未加入成员表）
     *    Merge babies created by user (may not have membership yet)
     * 3. 批量查询宝宝信息，按 ID 排序，组装为 DTO 列表返回
     *    Batch query baby info, sort by ID, assemble as DTO list
     *
     * @return 宝宝信息 DTO 列表 / list of baby info DTOs
     */
    @Override
    public List<BabyInfoDto> listBabies() {
        Long userId = AuthContext.requireCurrentUserId();
        Map<Long, BabyMember> memberByBabyId = listActiveMembersForUser(userId).stream()
                .collect(Collectors.toMap(BabyMember::getBabyId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Baby> ownedBabies = babyMapper.selectList(new LambdaQueryWrapper<Baby>()
                .eq(Baby::getUserId, userId)
                .orderByAsc(Baby::getId));
        ownedBabies.forEach(baby -> memberByBabyId.putIfAbsent(baby.getId(), findMember(userId, baby.getId())));
        if (memberByBabyId.isEmpty()) {
            return new ArrayList<>();
        }
        return babyMapper.selectBatchIds(memberByBabyId.keySet()).stream()
                .sorted(Comparator.comparing(Baby::getId))
                .map(baby -> toDto(baby, memberByBabyId.get(baby.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 创建宝宝
     * Create a new baby
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID为宝宝的主用户
     *    Get current user ID as the baby owner
     * 2. 构建 Baby 实体，对名字/头像/性别/生日进行默认化处理
     *    Build Baby entity with defaults for name/avatar/gender/birthDate
     * 3. 插入数据库得到 babyId
     *    Persist baby to DB to get auto-generated ID
     * 4. 为创建者自动创建家庭成员记录（owner=true），规范化角色名称
     *    Auto-create family membership for creator (owner=true), normalize role name
     * 5. 将创建者名下其他宝宝的家庭成员同步到新宝宝
     *    Copy existing family members from creator's other babies to the new baby
     * 6. 返回含成员角色的宝宝 DTO
     *    Return baby DTO with member role info
     *
     * @param request 创建宝宝请求 / create baby request
     * @return 创建后的宝宝 DTO / created baby DTO
     */
    @Override
    public BabyInfoDto createBaby(CreateBabyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = AuthContext.requireCurrentUserId();
        Baby baby = new Baby();
        baby.setUserId(userId);
        baby.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : "宝宝");
        baby.setAvatar(request.getAvatar() == null ? "" : request.getAvatar());
        baby.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : "boy");
        baby.setBirthDate(parseDate(request.getBirthDate(), null));
        baby.setCreatedAt(now);
        baby.setUpdatedAt(now);
        babyMapper.insert(baby);
        BabyMember member = createMemberIfMissing(
                baby.getId(),
                userId,
                normalizeRoleCode(request.getRoleCode(), userId),
                normalizeRoleName(request.getRoleCode(), request.getRoleName(), userId),
                true
        );
        copyOwnedFamilyMembersToBaby(userId, baby.getId(), userId);
        return toDto(baby, member);
    }

    /**
     * 更新宝宝信息
     * Update baby information
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID，验证用户对该宝宝有访问/编辑权限
     *    Get current user ID and validate user has access to the baby
     * 2. 部分更新：仅覆盖请求中非 null 的字段
     *    Partial update: only overwrite non-null fields from request
     * 3. 更新 updatedAt 并持久化
     *    Update updatedAt and persist
     * 4. 返回更新后的宝宝 DTO
     *    Return updated baby DTO
     *
     * @param request 更新宝宝请求 / update baby request
     * @return 更新后的宝宝 DTO / updated baby DTO
     */
    @Override
    public BabyInfoDto updateBaby(UpdateBabyRequest request) {
        Long userId = AuthContext.requireCurrentUserId();
        Baby baby = findBabyForUpdate(request.getId(), userId);
        if (request.getName() != null) {
            baby.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            baby.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            baby.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            baby.setBirthDate(parseDate(request.getBirthDate(), baby.getBirthDate()));
        }
        baby.setUpdatedAt(LocalDateTime.now());
        babyMapper.updateById(baby);
        return toDto(baby, findMember(userId, baby.getId()));
    }

    /**
     * 获取当前用户可访问的所有宝宝的家庭成员列表
     * Get family member list for all babies accessible to the current user
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 调用 listBabies 获取当前用户可访问的宝宝列表
     *    Call listBabies to get all babies accessible to current user
     * 2. 查询这些宝宝的所有活跃家庭成员记录，按宝宝和主人优先排序
     *    Query all active family members for those babies, ordered by babyId and owner first
     * 3. 批量加载成员用户信息（昵称/头像）
     *    Batch load member user info (nickname/avatar)
     * 4. 按宝宝分组，构建 BabyFamilyDto 列表返回
     *    Group by baby, build BabyFamilyDto list and return
     *
     * @return 家庭成员DTO列表 / family member DTO list
     */
    @Override
    public List<BabyFamilyDto> listFamilyMembers() {
        Long userId = AuthContext.requireCurrentUserId();
        List<BabyInfoDto> babies = listBabies();
        if (babies.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> babyIds = babies.stream().map(BabyInfoDto::getId).collect(Collectors.toList());
        Map<Long, Baby> babyById = babyMapper.selectBatchIds(babyIds).stream()
                .collect(Collectors.toMap(Baby::getId, item -> item));
        List<BabyMember> members = babyMemberMapper.selectList(new LambdaQueryWrapper<BabyMember>()
                .in(BabyMember::getBabyId, babyIds)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .orderByAsc(BabyMember::getBabyId)
                .orderByDesc(BabyMember::getOwner)
                .orderByAsc(BabyMember::getId));
        Set<Long> userIds = members.stream().map(BabyMember::getUserId).collect(Collectors.toSet());
        Map<Long, AppUser> userById = userIds.isEmpty()
                ? new LinkedHashMap<>()
                : appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, item -> item));

        Map<Long, BabyFamilyDto> familyByBabyId = new LinkedHashMap<>();
        babies.forEach(babyInfo -> {
            Baby baby = babyById.get(babyInfo.getId());
            if (baby == null) {
                return;
            }
            BabyFamilyDto family = new BabyFamilyDto();
            family.setBabyId(baby.getId());
            family.setBabyName(baby.getName());
            family.setBabyAvatar(baby.getAvatar());
            family.setBabyGender(baby.getGender());
            family.setBirthDate(baby.getBirthDate() == null ? null : baby.getBirthDate().toString());
            familyByBabyId.put(baby.getId(), family);
        });
        members.forEach(member -> {
            BabyFamilyDto family = familyByBabyId.get(member.getBabyId());
            if (family != null) {
                family.getMembers().add(toMemberDto(member, userById.get(member.getUserId()), userId));
            }
        });
        return new ArrayList<>(familyByBabyId.values());
    }

    /**
     * 更新当前用户在指定宝宝家庭中的角色
     * Update the current user's role in the specified baby's family
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 验证 babyId 不为空，查找宝宝并验证访问权限
     *    Validate babyId, find baby and verify access
     * 2. 查找当前用户的家庭成员记录
     *    Find current user's family membership record
     * 3. 若成员记录不存在则新建；否则更新角色信息
     *    Create membership if not exists; otherwise update role info
     * 4. 返回更新后的成员 DTO
     *    Return updated member DTO
     *
     * @param request 更新角色请求（babyId、roleCode、roleName）/ update role request
     * @return 更新后的成员 DTO / updated member DTO
     */
    @Override
    public BabyMemberDto updateMyFamilyRole(UpdateBabyMemberRoleRequest request) {
        Long userId = AuthContext.requireCurrentUserId();
        if (request.getBabyId() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "babyId is required");
        }
        Baby baby = findBabyForUser(request.getBabyId(), userId);
        if (baby == null) {
            throw new ResponseStatusException(NOT_FOUND, "Baby not found: " + request.getBabyId());
        }
        BabyMember member = findMember(userId, baby.getId());
        if (member == null) {
            member = createMemberIfMissing(
                    baby.getId(),
                    userId,
                    normalizeRoleCode(request.getRoleCode(), userId),
                    normalizeRoleName(request.getRoleCode(), request.getRoleName(), userId),
                    Objects.equals(baby.getUserId(), userId)
            );
        } else {
            member.setRoleCode(normalizeRoleCode(request.getRoleCode(), userId));
            member.setRoleName(normalizeRoleName(request.getRoleCode(), request.getRoleName(), userId));
            member.setUpdatedAt(LocalDateTime.now());
            babyMemberMapper.updateById(member);
        }
        return toMemberDto(member, appUserMapper.selectById(userId), userId);
    }

    /**
     * 通过宝宝 ID（邀请码）加入家庭
     * Join a family via baby ID (invitation code)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 验证 babyId 非空，查找宝宝实体（不检查访问权限，任意宝宝均可加入）
     *    Validate babyId, find baby (no access check, any baby can be joined)
     * 2. 规范化角色信息
     *    Normalize role code and role name
     * 3. 遍历宝宝主人名下的所有宝宝，为当前用户创建家庭成员记录（建立共同家庭关系）
     *    Iterate all babies owned by the baby's owner; create membership records (shared family)
     * 4. 返回目标宝宝的成员 DTO
     *    Return member DTO for the target baby
     *
     * @param request 加入家庭请求（babyId、roleCode、roleName）/ join family request
     * @return 成员 DTO / member DTO
     */
    @Override
    public BabyMemberDto joinFamilyViaInvite(JoinFamilyRequest request) {
        Long userId = AuthContext.requireCurrentUserId();
        if (request.getBabyId() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "babyId is required");
        }
        Baby baby = babyMapper.selectById(request.getBabyId());
        if (baby == null) {
            throw new ResponseStatusException(NOT_FOUND, "Baby not found: " + request.getBabyId());
        }
        String roleCode = normalizeRoleCode(request.getRoleCode(), userId);
        String roleName = normalizeRoleName(request.getRoleCode(), request.getRoleName(), userId);
        BabyMember member = null;
        for (Baby familyBaby : listOwnedBabies(baby.getUserId())) {
            BabyMember created = createMemberIfMissing(familyBaby.getId(), userId, roleCode, roleName, false);
            if (Objects.equals(familyBaby.getId(), baby.getId())) {
                member = created;
            }
        }
        if (member == null) {
            member = createMemberIfMissing(baby.getId(), userId, roleCode, roleName, false);
        }
        return toMemberDto(member, appUserMapper.selectById(userId), userId);
    }

    /**
     * 获取宝宝的当前成长阶段信息
     * Get current growth stage information for the specified baby
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 若传入 babyId 则直接查询，否则取当前用户的第一个宝宝
     *    If babyId provided use it directly; otherwise find user's first baby
     * 2. 如果宝宝不存在或未填写生日则返回 null
     *    Return null if baby not found or birthDate not set
     * 3. 计算宝宝出生天数，在 baby_growth_stage 表中匹配对应阶段
     *    Calculate age in days, match corresponding growth stage in baby_growth_stage table
     * 4. 将阶段信息组装为 DTO 返回
     *    Assemble and return growth stage DTO
     *
     * @param babyId 宝宝 ID（可为 null，自动取第一个）/ baby ID (nullable, defaults to first baby)
     * @return 成长阶段 DTO，找不到时返回 null / growth stage DTO, null if not found
     */
    @Override
    public BabyGrowthStageDto getGrowthStage(Long babyId) {
        Long userId = AuthContext.requireCurrentUserId();
        Baby baby;
        if (babyId != null) {
            baby = babyMapper.selectById(babyId);
        } else {
            baby = findFirstBaby(userId);
        }
        if (baby == null || baby.getBirthDate() == null) {
            return null;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(baby.getBirthDate(), LocalDate.now());
        if (days < 0) days = 0;
        BabyGrowthStage stage = babyGrowthStageMapper.selectOne(new LambdaQueryWrapper<BabyGrowthStage>()
                .le(BabyGrowthStage::getStartDay, (int) days)
                .gt(BabyGrowthStage::getEndDay, (int) days)
                .orderByAsc(BabyGrowthStage::getStartDay)
                .last("limit 1"));
        if (stage == null) {
            return null;
        }
        BabyGrowthStageDto dto = new BabyGrowthStageDto();
        dto.setId(stage.getId());
        dto.setStartDay(stage.getStartDay());
        dto.setEndDay(stage.getEndDay());
        dto.setStageName(stage.getStageName());
        dto.setSubtitle(stage.getSubtitle());
        dto.setPhysicalChanges(stage.getPhysicalChanges());
        return dto;
    }

    /**
     * 获取用于更新的宝宝实体，如果没有则抛出 404
     * Find baby for update; throw 404 if not found or user has no access
     */
    private Baby findBabyForUpdate(Long id, Long userId) {
        Long babyId = id == null ? getRequestedBabyId() : id;
        Baby baby = babyId == null ? findFirstBaby(userId) : findBabyForUser(babyId, userId);
        if (baby != null) {
            return baby;
        }
        throw new org.springframework.web.server.ResponseStatusException(NOT_FOUND, "Baby not found: " + (id != null ? id : "current"));
    }

    /**
     * 查找用户有访问权的第一个宝宝
     * Find the first baby accessible to the user
     * <p>
     * 优先查找成员表中最小 babyId 的宝宝，没有则回退到用户创建的第一个宝宝
     * First looks in membership table by smallest babyId; falls back to first baby created by user
     */
    private Baby findFirstBaby(Long userId) {
        BabyMember member = babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .orderByAsc(BabyMember::getBabyId)
                .last("limit 1"));
        if (member != null) {
            Baby baby = babyMapper.selectById(member.getBabyId());
            if (baby != null) {
                return baby;
            }
        }
        return babyMapper.selectOne(new LambdaQueryWrapper<Baby>()
                .eq(Baby::getUserId, userId)
                .orderByAsc(Baby::getId)
                .last("limit 1"));
    }

    /**
     * 查找用户有权限访问的指定宝宝，没有权限或宝宝不存在则返回 null
     * Find baby accessible to the user by ID; return null if not found or no access
     */
    private Baby findBabyForUser(Long babyId, Long userId) {
        Baby baby = babyMapper.selectById(babyId);
        if (baby == null) {
            return null;
        }
        if (hasBabyAccess(babyId, userId)) {
            return baby;
        }
        return null;
    }

    /**
     * 从 HTTP 请求头 X-Baby-Id 或查询参数 babyId 中获取用户指定的宝宝ID
     * Get the user-specified babyId from request header X-Baby-Id or query parameter babyId
     */
    private Long getRequestedBabyId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String raw = request.getHeader("X-Baby-Id");
        if (!StringUtils.hasText(raw)) {
            raw = request.getParameter("babyId");
        }
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid babyId: " + raw);
        }
    }

    /**
     * 查询用户的所有活跃家庭成员记录，按 babyId 和 id 正序排序
     * Query all active family membership records for the user, ordered by babyId and id ASC
     */
    private List<BabyMember> listActiveMembersForUser(Long userId) {
        return babyMemberMapper.selectList(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .orderByAsc(BabyMember::getBabyId)
                .orderByAsc(BabyMember::getId));
    }

    /**
     * 查询指定用户名下创建的所有宝宝，按 ID 正序排序
     * Query all babies created by the specified user, ordered by ID ASC
     */
    private List<Baby> listOwnedBabies(Long userId) {
        return babyMapper.selectList(new LambdaQueryWrapper<Baby>()
                .eq(Baby::getUserId, userId)
                .orderByAsc(Baby::getId));
    }

    /**
     * 将宝宝主人名下其他宝宝的家庭成员同步到新宝宝
     * Copy existing family members from owner's other babies to the newly created baby
     * <p>
     * 创建新宝宝时调用，保证家庭内其他成员自动关联到新宝宝
     * Called on baby creation to ensure existing family members are linked to the new baby
     */
    private void copyOwnedFamilyMembersToBaby(Long ownerUserId, Long babyId, Long creatorUserId) {
        List<Long> existingBabyIds = listOwnedBabies(ownerUserId).stream()
                .map(Baby::getId)
                .filter(id -> !Objects.equals(id, babyId))
                .collect(Collectors.toList());
        if (existingBabyIds.isEmpty()) {
            return;
        }
        Map<Long, BabyMember> memberByUserId = babyMemberMapper.selectList(new LambdaQueryWrapper<BabyMember>()
                .in(BabyMember::getBabyId, existingBabyIds)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .orderByAsc(BabyMember::getId)).stream()
                .collect(Collectors.toMap(BabyMember::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));
        memberByUserId.values().stream()
                .filter(member -> !Objects.equals(member.getUserId(), creatorUserId))
                .forEach(member -> createMemberIfMissing(
                        babyId,
                        member.getUserId(),
                        member.getRoleCode(),
                        member.getRoleName(),
                        false
                ));
    }

    /**
     * 查找用户在指定宝宝中的活跃家庭成员记录
     * Find the active membership record for a user in a specified baby's family
     */
    private BabyMember findMember(Long userId, Long babyId) {
        if (userId == null || babyId == null) {
            return null;
        }
        return babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getBabyId, babyId)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .last("limit 1"));
    }

    /**
     * 查找用户最后加入的一个活跃家庭成员记录（用于默认角色推断）
     * Find the most recently joined active membership record for the user (used to infer default role)
     */
    private BabyMember findLastMemberForUser(Long userId) {
        return babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getUserId, userId)
                .eq(BabyMember::getStatus, ACTIVE_STATUS)
                .orderByDesc(BabyMember::getId)
                .last("limit 1"));
    }

    /**
     * 判断用户是否有权访问宝宝：家庭成员记录存在或用户为宝宝的主用户
     * Check if user has access to baby: either has active membership or is the baby owner
     */
    private boolean hasBabyAccess(Long babyId, Long userId) {
        BabyMember member = findMember(userId, babyId);
        if (member != null) {
            return true;
        }
        Baby baby = babyMapper.selectById(babyId);
        return baby != null && Objects.equals(baby.getUserId(), userId);
    }

    /**
     * 创建家庭成员记录，如果已存在则更新角色信息并激活（幂等操作）
     * Create family membership record if not exists; otherwise update role and re-activate (idempotent)
     */
    private BabyMember createMemberIfMissing(Long babyId, Long userId, String roleCode, String roleName, boolean owner) {
        LocalDateTime now = LocalDateTime.now();
        BabyMember member = babyMemberMapper.selectOne(new LambdaQueryWrapper<BabyMember>()
                .eq(BabyMember::getBabyId, babyId)
                .eq(BabyMember::getUserId, userId)
                .last("limit 1"));
        if (member == null) {
            member = new BabyMember();
            member.setBabyId(babyId);
            member.setUserId(userId);
            member.setCreatedAt(now);
            member.setOwner(owner);
            member.setRoleCode(roleCode);
            member.setRoleName(roleName);
            member.setStatus(ACTIVE_STATUS);
            member.setUpdatedAt(now);
            babyMemberMapper.insert(member);
            return member;
        }
        member.setRoleCode(roleCode);
        member.setRoleName(roleName);
        member.setStatus(ACTIVE_STATUS);
        member.setOwner(Boolean.TRUE.equals(member.getOwner()) || owner);
        member.setUpdatedAt(now);
        babyMemberMapper.updateById(member);
        return member;
    }

    /**
     * 规范化角色编码：如果请求非空则使用请求值，否则尝试从用户历史推断，最后默认 guardian
     * Normalize role code: use request value if non-empty; infer from user history; default to "guardian"
     */
    private String normalizeRoleCode(String roleCode, Long userId) {
        if (StringUtils.hasText(roleCode)) {
            return roleCode.trim();
        }
        BabyMember lastMember = findLastMemberForUser(userId);
        if (lastMember != null && StringUtils.hasText(lastMember.getRoleCode())) {
            return lastMember.getRoleCode();
        }
        return DEFAULT_ROLE_CODE;
    }

    /**
     * 规范化角色名称：优先使用请求中的 roleName，其次按 roleCode 推导，封退到历史记录或默认“家人”
     * Normalize role name: prefer request roleName, then infer from roleCode, fallback to history or "家人"
     */
    private String normalizeRoleName(String roleCode, String roleName, Long userId) {
        if (StringUtils.hasText(roleName)) {
            return roleName.trim();
        }
        if (StringUtils.hasText(roleCode)) {
            return roleNameForCode(roleCode.trim());
        }
        BabyMember lastMember = findLastMemberForUser(userId);
        if (lastMember != null && StringUtils.hasText(lastMember.getRoleName())) {
            return lastMember.getRoleName();
        }
        return DEFAULT_ROLE_NAME;
    }

    /**
     * 将标准角色编码映射为中文角色名称
     * Map standard role code to Chinese role name
     */
    private String roleNameForCode(String roleCode) {
        if (roleCode == null) return DEFAULT_ROLE_NAME;
        switch (roleCode) {
            case "father": return "爸爸";
            case "mother": return "妈妈";
            case "grandpa": return "爷爷";
            case "grandma": return "奶奶";
            case "maternalGrandpa": return "外公";
            case "maternalGrandma": return "外婆";
            case "uncle": return "叔叔";
            case "aunt": return "阿姨";
            case "paternalAunt": return "姑姑";
            case "maternalUncle": return "舅舅";
            case "nanny": return "保姆";
            case "other": return "其他";
            default: return roleCode.startsWith("maternal") ? roleCode : DEFAULT_ROLE_NAME;
        }
    }

    /**
     * 将 BabyMember 实体转换为成员 DTO，包含用户昵称/头像/角色/是否为自己
     * Convert BabyMember entity to member DTO, including nickname/avatar/role/mine flag
     */
    private BabyMemberDto toMemberDto(BabyMember member, AppUser user, Long currentUserId) {
        BabyMemberDto dto = new BabyMemberDto();
        dto.setMemberId(member.getId());
        dto.setBabyId(member.getBabyId());
        dto.setUserId(member.getUserId());
        dto.setNickName(user == null ? "" : user.getNickName());
        dto.setAvatarUrl(user == null ? "" : user.getAvatarUrl());
        dto.setRoleCode(member.getRoleCode());
        dto.setRoleName(member.getRoleName());
        dto.setOwner(Boolean.TRUE.equals(member.getOwner()));
        dto.setMine(Objects.equals(member.getUserId(), currentUserId));
        if (user != null && user.getUpdatedAt() != null) {
            dto.setLastLoginTime(user.getUpdatedAt().toString());
        }
        return dto;
    }

    /**
     * 将 Baby 实体转换为宝宝信息 DTO，自动计算年龄并填充家庭角色
     * Convert Baby entity to baby info DTO, auto-calculating age and filling family role
     */
    private BabyInfoDto toDto(Baby baby, BabyMember member) {
        BabyInfoDto dto = new BabyInfoDto();
        dto.setId(baby.getId());
        dto.setName(baby.getName());
        dto.setAvatar(baby.getAvatar());
        dto.setGender(baby.getGender());
        dto.setBirthDate(baby.getBirthDate() == null ? null : baby.getBirthDate().toString());
        dto.setAge(calcBabyAge(baby.getBirthDate()));
        if (member != null) {
            dto.setFamilyRoleCode(member.getRoleCode());
            dto.setFamilyRoleName(member.getRoleName());
        }
        return dto;
    }

    /**
     * 根据出生日期计算宝宝年龄字符串，格式：X岁X个月X天 或 X个月X天 或 X天
     * Calculate baby age string from birthDate: "X岁X个月X天" or "X个月X天" or "X天"
     */
    private String calcBabyAge(LocalDate birthDate) {
        if (birthDate == null) {
            return "";
        }
        Period period = Period.between(birthDate, LocalDate.now());
        if (period.getYears() > 0) {
            return period.getYears() + "岁" + period.getMonths() + "个月" + period.getDays() + "天";
        }
        if (period.getMonths() > 0) {
            return period.getMonths() + "个月" + period.getDays() + "天";
        }
        return period.getDays() + "天";
    }

    /**
     * 将日期字符串解析为 LocalDate，如果为空则返回备用值
     * Parse date string to LocalDate; return fallback if empty
     */
    private LocalDate parseDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

}
