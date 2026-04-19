package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {
    private String realName;
    private String nickname;
    private String phone;
    private String email;
    private String roleCode;
    private Long shopId;
    private Integer status;
}
