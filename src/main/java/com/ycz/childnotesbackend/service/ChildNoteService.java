package com.ycz.childnotesbackend.service;

import com.ycz.childnotesbackend.model.dto.CreateChildNoteRequest;
import com.ycz.childnotesbackend.model.entity.ChildNote;

public interface ChildNoteService {

    /**
     * 创建育儿笔记
     * Create a child note
     *
     * @param request 创建笔记请求参数 / create note request parameters
     * @return 创建的笔记 / created note
     */
    ChildNote createNote(CreateChildNoteRequest request);
}

