package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionCommentRequest;
import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionPostRequest;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionCommentDto;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionPostDto;

import java.util.List;

public interface DiscussionService {

    /**
     * 获取帖子列表
     * Get list of discussion posts
     *
     * @param category 分类 / category
     * @param keyword  关键词 / keyword
     * @return 帖子列表 / list of posts
     */
    List<DiscussionPostDto> listPosts(String category, String keyword);

    /**
     * 获取帖子详情
     * Get discussion post detail
     *
     * @param id 帖子ID / post ID
     * @return 帖子详情 / post detail
     */
    DiscussionPostDto getPost(Long id);

    /**
     * 创建帖子
     * Create a discussion post
     *
     * @param request 创建帖子请求参数 / create post request parameters
     * @return 创建的帖子 / created post
     */
    DiscussionPostDto createPost(CreateDiscussionPostRequest request);

    /**
     * 获取评论列表
     * Get list of comments for a post
     *
     * @param postId 帖子ID / post ID
     * @return 评论列表 / list of comments
     */
    List<DiscussionCommentDto> listComments(Long postId);

    /**
     * 创建评论
     * Create a comment for a post
     *
     * @param postId  帖子ID / post ID
     * @param request 创建评论请求参数 / create comment request parameters
     * @return 创建的评论 / created comment
     */
    DiscussionCommentDto createComment(Long postId, CreateDiscussionCommentRequest request);
}
