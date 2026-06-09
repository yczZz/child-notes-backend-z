package com.ycz.childnotesbackend.service.impl;

import com.ycz.childnotesbackend.mapper.ChildNoteMapper;
import com.ycz.childnotesbackend.model.dto.CreateChildNoteRequest;
import com.ycz.childnotesbackend.model.entity.ChildNote;
import com.ycz.childnotesbackend.service.ChildNoteService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChildNoteServiceImpl implements ChildNoteService {

    private final ChildNoteMapper childNoteMapper;

    public ChildNoteServiceImpl(ChildNoteMapper childNoteMapper) {
        this.childNoteMapper = childNoteMapper;
    }

    /**
     * 创建育儿笔记
     * Create a child note
     * <p>
     * 逻辑流程 / Logic flow:
     * 1. 获取当前系统时间作为创建时间和更新时间
     *    Get current system time as createdAt and updatedAt
     * 2. 构建 ChildNote 实体，填充孩子名、标题、内容、笔记日期
     *    Build ChildNote entity from request (childName, title, content, noteDate)
     * 3. 插入数据库， MyBatis-Plus 自动回写自增主键 ID
     *    Persist to DB; MyBatis-Plus auto-fills the generated primary key ID
     * 4. 返回含有ID的实体
     *    Return the entity with auto-generated ID
     *
     * @param request 创建笔记请求（孩子名、标题、内容、笔记日期）/ create note request
     * @return 已创建的 ChildNote 实体（含 ID）/ created ChildNote entity (with ID)
     */
    @Override
    public ChildNote createNote(CreateChildNoteRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ChildNote childNote = new ChildNote();
        childNote.setChildName(request.getChildName());
        childNote.setTitle(request.getTitle());
        childNote.setContent(request.getContent());
        childNote.setNoteDate(request.getNoteDate());
        childNote.setCreatedAt(now);
        childNote.setUpdatedAt(now);
        childNoteMapper.insert(childNote);
        return childNote;
    }
}
