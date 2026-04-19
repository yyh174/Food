package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShopStatusRequest implements Serializable {
    private Integer status;
}
