package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDetailVO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String realName;
    private String phone;
    private String email;
    private String roleCode;
    private String roleName;
    private Long shopId;
    private String shopName;
    private Long tenantId;
    private String tenantName;
    private Integer status;
    private String lastLoginTime;
    private String createdAt;
    private String updatedAt;
}
