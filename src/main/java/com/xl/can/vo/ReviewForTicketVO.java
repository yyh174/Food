package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewForTicketVO {
    private Long id;
    private Integer platform;
    private String platformOrderId;
    private Long shopId;
    private String shopName;
    private Integer starRating;
    private String content;
    private LocalDateTime createdAt;
    private Boolean alreadyLinked;
}
