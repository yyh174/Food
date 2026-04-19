package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class TenantCreateVO implements Serializable {
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String inviteCode;
    private Integer status;
}
