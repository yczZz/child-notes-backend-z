package com.ycz.childnotesbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ip_blacklist")
public class IpBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ipAddress;

    private String triggerMethod;

    private String triggerPath;

    private String triggerEndpoint;

    private Integer requestCount;

    private LocalDateTime windowStartedAt;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
