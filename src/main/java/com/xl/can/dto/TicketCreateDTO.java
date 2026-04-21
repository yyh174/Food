package com.xl.can.dto;

import lombok.Data;

import java.util.List;

@Data
public class TicketCreateDTO {
    private String title;
    private String description;
    private Long typeId;
    private Long shopId;
    private String deadline;
    private List<String> images;
    private Long reviewId;
}
