package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_timeline")
public class TicketTimeline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;

    private Long tenantId;

    private String action;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
