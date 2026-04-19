package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCreateRequest implements Serializable {
    private String username;
    private String realName;
    private String phone;
    private String roleCode;
    private Long shopId;
    private String password;
}
