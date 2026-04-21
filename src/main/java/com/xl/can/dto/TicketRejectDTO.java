package com.xl.can.dto;

import lombok.Data;

@Data
public class TicketRejectDTO {
    private String content;
    private String deadline; // 可选，更新截止时间
}
