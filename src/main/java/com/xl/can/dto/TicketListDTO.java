package com.xl.can.dto;

import lombok.Data;

@Data
public class TicketListDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Long shopId;
    private Long typeId;
    private Integer status;
    private String viewAs;
    private String keyword;
    private String startDate;
    private String endDate;
}
