package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer platform;

    private String platformOrderId;

    private String platformReviewId;

    private Long shopId;

    private Long tenantId;

    private Integer starRating;

    private String content;

    private LocalDateTime orderTime;

    private String replyContent;

    private LocalDateTime replyTime;

    private Long replyUserId;

    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
