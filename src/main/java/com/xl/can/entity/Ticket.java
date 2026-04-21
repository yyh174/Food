package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private Long typeId;

    private Integer status;

    private Integer category;

    private Long shopId;

    private Long tenantId;

    private String title;

    private String description;

    private String detailData;

    private LocalDateTime deadline;

    private String solution;

    private Long reviewId;

    private Long creatorId;

    private Long assigneeId;

    private String priority;

    private String images;

    private String suggestion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Integer deletedAt;
}
