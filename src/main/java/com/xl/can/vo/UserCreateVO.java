package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserCreateVO implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private Integer status;
    private LocalDateTime createdAt;
}
