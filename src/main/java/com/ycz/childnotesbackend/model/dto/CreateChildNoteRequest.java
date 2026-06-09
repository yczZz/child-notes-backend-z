package com.ycz.childnotesbackend.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class CreateChildNoteRequest {

    @NotBlank(message = "childName不能为空")
    @Size(max = 64, message = "childName不能超过64个字符")
    private String childName = "test-child";

    @NotBlank(message = "title不能为空")
    @Size(max = 128, message = "title不能超过128个字符")
    private String title = "测试笔记";

    @Size(max = 2000, message = "content不能超过2000个字符")
    private String content = "第一条测试笔记";

    private LocalDate noteDate = LocalDate.now();
}

