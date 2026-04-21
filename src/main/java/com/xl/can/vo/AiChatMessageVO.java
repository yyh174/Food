package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
