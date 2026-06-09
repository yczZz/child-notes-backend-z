package com.ycz.childnotesbackend.controller;

import com.ycz.childnotesbackend.model.base.Response;
import com.ycz.childnotesbackend.model.base.ResponseStateFactory;
import com.ycz.childnotesbackend.model.dto.CreateChildNoteRequest;
import com.ycz.childnotesbackend.model.entity.ChildNote;
import com.ycz.childnotesbackend.service.ChildNoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/test")
public class TestController {

    private final ChildNoteService childNoteService;

    public TestController(ChildNoteService childNoteService) {
        this.childNoteService = childNoteService;
    }

    @PostMapping("/note")
    public Response<ChildNote> createNote(@Valid @RequestBody CreateChildNoteRequest request) {
        log.info("createNote: {}", request);
        try {
            return new Response<>(childNoteService.createNote(request));
        } catch (Exception e) {
            log.error("createNote error: {}", e.getMessage(), e);
            return new Response<>(ResponseStateFactory.getFail().state(), e.getMessage());
        }
    }
}
