package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    private Long id;
    private Long shopId;
    private String shopName;
    private Integer platform;
    private String platformOrderId;
    private Integer starRating;
    private String content;
    private List<String> tags;
    private Integer sentimentType;
    private Integer replyStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime orderTime;
    private LocalDateTime createdAt;
}
