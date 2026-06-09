package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionCommentRequest;
import com.ycz.childnotesbackend.model.dto.discussion.CreateDiscussionPostRequest;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionCommentDto;
import com.ycz.childnotesbackend.model.dto.discussion.DiscussionPostDto;
import com.ycz.childnotesbackend.service.DiscussionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/posts")
    public Response<List<DiscussionPostDto>> listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return new Response<>(discussionService.listPosts(category, keyword));
    }

    @GetMapping("/posts/{id}")
    public Response<DiscussionPostDto> getPost(@PathVariable Long id) {
        return new Response<>(discussionService.getPost(id));
    }

    @PostMapping("/posts")
    public Response<DiscussionPostDto> createPost(@RequestBody CreateDiscussionPostRequest request) {
        return new Response<>(discussionService.createPost(request));
    }

    @GetMapping("/posts/{postId}/comments")
    public Response<List<DiscussionCommentDto>> listComments(@PathVariable Long postId) {
        return new Response<>(discussionService.listComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public Response<DiscussionCommentDto> createComment(
            @PathVariable Long postId,
            @RequestBody CreateDiscussionCommentRequest request) {
        return new Response<>(discussionService.createComment(postId, request));
    }
}
