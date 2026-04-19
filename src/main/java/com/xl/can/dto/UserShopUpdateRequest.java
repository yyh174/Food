package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserShopUpdateRequest implements Serializable {
    private Long shopId;
}
