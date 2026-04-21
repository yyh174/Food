package com.xl.can.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewListDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    private Integer platform;

    private Long shopId;

    private Integer replyStatus;

    private String keyword;

    private LocalDate startDate;

    private LocalDate endDate;
}
