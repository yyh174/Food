package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShopUpdateRequest implements Serializable {
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
}
