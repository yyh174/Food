package com.xl.can.vo;

import lombok.Data;

@Data
public class TicketTypeVO {
    private Long id;
    private String name;
    private String type;
    private Boolean isDefault;
    private Boolean supportReview;
}
