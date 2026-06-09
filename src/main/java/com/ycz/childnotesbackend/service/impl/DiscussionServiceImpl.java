package com.ycz.childnotesbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycz.childnotesbackend.context.AuthContext;
import com.ycz.childnotesbackend.mapper.AppUserMapper;
import com.ycz.childnotesbackend.mapper.DiscussionCommentMapper;
import com.ycz.childnotesbackend.mapper.DiscussionPostMapper;
import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionCommentRequest;
import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionPostRequest;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionCommentDto;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionPostDto;
import com.ycz.childnotesbackend.model.entity.AppUser;
import com.ycz.childnotesbackend.model.entity.DiscussionComment;
import com.ycz.childnotesbackend.model.entity.DiscussionPost;
import com.ycz.childnotesbackend.service.DiscussionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DiscussionServiceImpl implements DiscussionService {

    private static final int MAX_IMAGE_COUNT = 4;

    private static final DateTimeFormatter FRONT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("daily", "日常状态");
        CATEGORY_NAMES.put("feeding", "喂养吐奶");
        CATEGORY_NAMES.put("sleep", "睡眠作息");
        CATEGORY_NAMES.put("diaper", "便便尿布");
        CATEGORY_NAMES.put("fever", "发热不适");
        CATEGORY_NAMES.put("growth", "发育成长");
        CATEGORY_NAMES.put("care", "护理经验");
        CATEGORY_NAMES.put("other", "其他");
    }

    private final DiscussionPostMapper discussionPostMapper;

    private final DiscussionCommentMapper discussionCommentMapper;

    private final AppUserMapper appUserMapper;

    private final ObjectMapper objectMapper;

    public DiscussionServiceImpl(
            DiscussionPostMapper discussionPostMapper,
            DiscussionCommentMapper discussionCommentMapper,
            AppUserMapper appUserMapper,
            ObjectMapper objectMapper) {
        this.discussionPostMapper = discussionPostMapper;
        this.discussionCommentMapper = discussionCommentMapper;
        this.appUserMapper = appUserMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询讨论帖列表，支持按分类和标题关键词过滤
     * List discussion posts with optional category and keyword filter
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID，用于标识水贴是否属于自己
     *    Get current user ID to determine post ownership
     * 2. 构建动态查询：如果 category 非空且非 "all" 则限制分类；如果 keyword 非空则按标题 LIKE 过滤
     *    Build dynamic query: filter by category if non-empty and not "all"; filter by title LIKE if keyword provided
     * 3. 按创建时间倒序查询，最多返回 100 条
     *    Query ordered by createdAt DESC, limit 100
     * 4. 批量加载帖子所属用户信息，组装为 DTO 返回
     *    Batch load user info for post authors, assemble and return as DTOs
     *
     * @param category 分类编码（不传或传 "all" 表示不限制）/ category code (omit or "all" for no filter)
     * @param keyword  标题关键词 / title keyword
     * @return 帖子DTO列表 / list of post DTOs
     */
    @Override
    public List<DiscussionPostDto> listPosts(String category, String keyword) {
        Long currentUserId = AuthContext.requireCurrentUserId();
        LambdaQueryWrapper<DiscussionPost> wrapper = new LambdaQueryWrapper<DiscussionPost>()
                .orderByDesc(DiscussionPost::getCreatedAt)
                .orderByDesc(DiscussionPost::getId)
                .last("limit 100");
        String safeCategory = trim(category);
        if (StringUtils.hasText(safeCategory) && !"all".equals(safeCategory)) {
            wrapper.eq(DiscussionPost::getCategory, safeCategory);
        }
        String safeKeyword = trim(keyword);
        if (StringUtils.hasText(safeKeyword)) {
            wrapper.like(DiscussionPost::getTitle, safeKeyword);
        }

        List<DiscussionPost> posts = discussionPostMapper.selectList(wrapper);
        Map<Long, AppUser> users = loadUsers(posts.stream()
                .map(DiscussionPost::getUserId)
                .collect(Collectors.toSet()));
        return posts.stream()
                .map(post -> toPostDto(post, users.get(post.getUserId()), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取帖子详情
     * Get discussion post detail
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID用于赋值 mine 字段
     *    Get current user ID for setting the mine field
     * 2. 根据 ID 查询帖子，不存在则 404
     *    Find post by ID, throw 404 if not found
     * 3. 查询帖子作者信息，组装成 DTO 返回
     *    Load author user info and assemble into post DTO
     *
     * @param id 帖子ID / post ID
     * @return 帖子详情DTO / post detail DTO
     */
    @Override
    public DiscussionPostDto getPost(Long id) {
        Long currentUserId = AuthContext.requireCurrentUserId();
        DiscussionPost post = findPost(id);
        AppUser user = post.getUserId() == null ? null : appUserMapper.selectById(post.getUserId());
        return toPostDto(post, user, currentUserId);
    }

    /**
     * 创建讨论帖子
     * Create a discussion post
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID作为帖子作者
     *    Get current user ID as post author
     * 2. 验证请求字段：标题、正文、分类均不能为空，分类必须在预定义列表中
     *    Validate: title, content, category are required; category must be in the predefined list
     * 3. 校验長度限制：标题最多 120 字，正文最多 4000 字
     *    Length check: title max 120 chars, content max 4000 chars
     * 4. 处理图片列表：去除无效 URL，最多允许 4 张
     *    Process images: filter invalid URLs, max 4 images allowed
     * 5. 构建帖子实体并插入数据库
     *    Build post entity and persist to DB
     * 6. 调用 getPost 反查询将帖子完整 DTO 返回
     *    Re-query via getPost to return full post DTO
     *
     * @param request 创建帖子请求（标题、内容、分类、图片）/ create post request
     * @return 帖子DTO / post DTO
     */
    @Override
    public DiscussionPostDto createPost(CreateDiscussionPostRequest request) {
        Long currentUserId = AuthContext.requireCurrentUserId();
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "发帖内容不能为空");
        }
        String title = trim(request.getTitle());
        String content = trim(request.getContent());
        String category = trim(request.getCategory());
        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入标题");
        }
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入正文");
        }
        if (!StringUtils.hasText(category)) {
            throw new ResponseStatusException(BAD_REQUEST, "请选择分类");
        }
        if (!CATEGORY_NAMES.containsKey(category)) {
            throw new ResponseStatusException(BAD_REQUEST, "分类不存在");
        }
        if (title.length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "标题最多120个字");
        }
        if (content.length() > 4000) {
            throw new ResponseStatusException(BAD_REQUEST, "正文最多4000个字");
        }

        LocalDateTime now = LocalDateTime.now();
        DiscussionPost post = new DiscussionPost();
        post.setUserId(currentUserId);
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setImagesJson(writeImages(normalizeImages(request.getImages())));
        post.setCommentCount(0);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        discussionPostMapper.insert(post);
        return getPost(post.getId());
    }

    /**
     * 获取指定帖子的评论列表
     * Get the comment list for a specified post
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID用于标识评论是否属于自己
     *    Get current user ID for setting mine field
     * 2. 检查帖子是否存在，不存在则 404
     *    Verify post exists, throw 404 if not found
     * 3. 按创建时间正序查询帖子下的全部评论，最多 200 条
     *    Query all comments for the post ordered by createdAt ASC, limit 200
     * 4. 批量加载评论作者信息，组装为 DTO 返回
     *    Batch load comment author user info, assemble as DTOs
     *
     * @param postId 帖子ID / post ID
     * @return 评论 DTO 列表（按时间正序）/ list of comment DTOs ordered by time ASC
     */
    @Override
    public List<DiscussionCommentDto> listComments(Long postId) {
        Long currentUserId = AuthContext.requireCurrentUserId();
        findPost(postId);
        List<DiscussionComment> comments = discussionCommentMapper.selectList(new LambdaQueryWrapper<DiscussionComment>()
                .eq(DiscussionComment::getPostId, postId)
                .orderByAsc(DiscussionComment::getCreatedAt)
                .orderByAsc(DiscussionComment::getId)
                .last("limit 200"));
        Map<Long, AppUser> users = loadUsers(comments.stream()
                .map(DiscussionComment::getUserId)
                .collect(Collectors.toSet()));
        return comments.stream()
                .map(comment -> toCommentDto(comment, users.get(comment.getUserId()), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 创建评论并更新帖子评论计数（事务性操作）
     * Create a comment and atomically increment post comment count (transactional)
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前用户ID作为评论作者
     *    Get current user ID as comment author
     * 2. 校验帖子存在；校验评论内容非空且不超过 1000 字
     *    Validate post exists; validate comment content is non-empty and <= 1000 chars
     * 3. 构建评论实体并插入数据库
     *    Build comment entity and persist to DB
     * 4. 使用 SQL 表达式将帖子的 comment_count + 1（原子更新）
     *    Atomically increment post comment_count via SQL expression
     * 5. 查询作者用户信息，组装评论 DTO 返回
     *    Load author user info and return comment DTO
     *
     * @param postId  帖子ID / post ID
     * @param request 创建评论请求（内容）/ create comment request (content)
     * @return 评论 DTO / comment DTO
     */
    @Override
    @Transactional
    public DiscussionCommentDto createComment(Long postId, CreateDiscussionCommentRequest request) {
        Long currentUserId = AuthContext.requireCurrentUserId();
        DiscussionPost post = findPost(postId);
        if (request == null || !StringUtils.hasText(trim(request.getContent()))) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入评论内容");
        }
        String content = trim(request.getContent());
        if (content.length() > 1000) {
            throw new ResponseStatusException(BAD_REQUEST, "评论最多1000个字");
        }

        LocalDateTime now = LocalDateTime.now();
        DiscussionComment comment = new DiscussionComment();
        comment.setPostId(postId);
        comment.setUserId(currentUserId);
        comment.setContent(content);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        discussionCommentMapper.insert(comment);

        discussionPostMapper.update(null, new LambdaUpdateWrapper<DiscussionPost>()
                .eq(DiscussionPost::getId, post.getId())
                .setSql("comment_count = comment_count + 1")
                .set(DiscussionPost::getUpdatedAt, now));

        AppUser user = appUserMapper.selectById(currentUserId);
        return toCommentDto(comment, user, currentUserId);
    }

    /**
     * 根据 ID 查找帖子，ID 为空或帖子不存在时抛出异常
     * Find post by ID; throw exception if ID is null or post not found
     */
    private DiscussionPost findPost(Long id) {
        if (id == null) {
            throw new ResponseStatusException(BAD_REQUEST, "帖子ID不能为空");
        }
        DiscussionPost post = discussionPostMapper.selectById(id);
        if (post == null) {
            throw new ResponseStatusException(NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    /**
     * 批量加载用户信息，将 userIds 映射为 id -> AppUser 的 Map
     * Batch load user information and return a Map of id -> AppUser
     * <p>
     * 自动过滤掉 null ID，若 ID 集合为空则直接返回空 Map
     * Null IDs are filtered; returns empty Map if IDs are empty
     */
    private Map<Long, AppUser> loadUsers(Set<Long> userIds) {
        Set<Long> safeIds = userIds == null ? Collections.emptySet() : userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return appUserMapper.selectList(new LambdaQueryWrapper<AppUser>().in(AppUser::getId, safeIds))
                .stream()
                .collect(Collectors.toMap(AppUser::getId, user -> user));
    }

    /**
     * 将 DiscussionPost 实体转换为帖子 DTO
     * Convert DiscussionPost entity to post DTO
     * <p>
     * 包含：帖子内容、分类名称、图片列表、评论数、作者信息及是否是自己的帖子
     * Includes: post content, category name, image list, comment count, author info, and mine flag
     */
    private DiscussionPostDto toPostDto(DiscussionPost post, AppUser user, Long currentUserId) {
        DiscussionPostDto dto = new DiscussionPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCategory(post.getCategory());
        dto.setCategoryName(CATEGORY_NAMES.getOrDefault(post.getCategory(), "其他"));
        dto.setImages(readImages(post.getImagesJson()));
        dto.setCommentCount(safeInt(post.getCommentCount()));
        dto.setAuthorId(post.getUserId());
        dto.setAuthorName(user == null || !StringUtils.hasText(user.getNickName()) ? "微信用户" : user.getNickName());
        dto.setAuthorAvatarUrl(user == null ? "" : user.getAvatarUrl());
        dto.setMine(Objects.equals(post.getUserId(), currentUserId));
        dto.setCreatedAt(formatTime(post.getCreatedAt()));
        dto.setUpdatedAt(formatTime(post.getUpdatedAt()));
        return dto;
    }

    /**
     * 将 DiscussionComment 实体转换为评论 DTO
     * Convert DiscussionComment entity to comment DTO
     * <p>
     * 包含：评论内容、作者信息及是否是自己的评论
     * Includes: comment content, author info, and mine flag
     */
    private DiscussionCommentDto toCommentDto(DiscussionComment comment, AppUser user, Long currentUserId) {
        DiscussionCommentDto dto = new DiscussionCommentDto();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setContent(comment.getContent());
        dto.setAuthorId(comment.getUserId());
        dto.setAuthorName(user == null || !StringUtils.hasText(user.getNickName()) ? "微信用户" : user.getNickName());
        dto.setAuthorAvatarUrl(user == null ? "" : user.getAvatarUrl());
        dto.setMine(Objects.equals(comment.getUserId(), currentUserId));
        dto.setCreatedAt(formatTime(comment.getCreatedAt()));
        dto.setUpdatedAt(formatTime(comment.getUpdatedAt()));
        return dto;
    }

    /**
     * 验证并规范化图片列表：去除空白项，超过 4 张则抛出 400
     * Validate and normalize image list: remove blank entries, throw 400 if more than 4 images
     */
    private List<String> normalizeImages(List<String> images) {
        if (images == null) {
            return new ArrayList<>();
        }
        List<String> safeImages = images.stream()
                .map(this::trim)
                .filter(StringUtils::hasText)
                .limit(MAX_IMAGE_COUNT + 1L)
                .collect(Collectors.toList());
        if (safeImages.size() > MAX_IMAGE_COUNT) {
            throw new ResponseStatusException(BAD_REQUEST, "图片最多上传4张");
        }
        return safeImages;
    }

    /**
     * 将图片 URL 列表从 JSON 字符串反序列化，读取失败时返回空列表
     * Deserialize image URL list from JSON string; return empty list on failure
     */
    private List<String> readImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 将图片 URL 列表序列化为 JSON 字符串存入数据库
     * Serialize image URL list to JSON string for storage
     */
    private String writeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images == null ? Collections.emptyList() : images);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write discussion images", e);
        }
    }

    /**
     * 将 LocalDateTime 格式化为前端展示用字符串（yyyy-MM-dd HH:mm）
     * Format LocalDateTime to front-end display string (yyyy-MM-dd HH:mm)
     */
    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(FRONT_TIME);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
