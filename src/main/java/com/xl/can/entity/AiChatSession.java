package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_session")
public class AiChatSession implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private String title;

    /**
     * 对话上下文摘要，当对话超过长度限制时生成
     */
    private String contextSummary;

    /**
     * 最后一次摘要后的消息数量，用于判断何时需要生成新摘要
     */
    private Integer messageCountAfterSummary;

    /**
     * 当前对话的 token 估算数量
     */
    private Integer estimatedTokens;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
