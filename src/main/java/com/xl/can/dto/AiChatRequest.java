package com.xl.can.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private Long sessionId;
}
