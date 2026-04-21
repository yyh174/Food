package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiChatSessionVO {
    private Long id;
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private List<AiChatMessageVO> messages;
}
