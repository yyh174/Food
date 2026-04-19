package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserListVO implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String roleCode;
    private String roleName;
    private Long shopId;
    private String shopName;
    private Integer status;
    private String lastLoginTime;
    private LocalDateTime createdAt;
}
