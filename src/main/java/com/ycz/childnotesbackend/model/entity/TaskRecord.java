package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_record")
public class TaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String taskType;

    private String taskKey;

    private Long relatedUserId;

    private Integer points;

    private String status;

    private String payloadJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
