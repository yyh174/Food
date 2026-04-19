package com.xl.can.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShopListVO implements Serializable {
    private Long id;
    private String shopCode;
    private String shopName;
    private String province;
    private String city;
    private String district;
    private String address;
    private String managerName;
    private String managerPhone;
    private String meituanId;
    private String eleId;
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
