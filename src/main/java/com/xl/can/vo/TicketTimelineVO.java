package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketTimelineVO {
    private Long id;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String content;
    private LocalDateTime createdAt;
}
