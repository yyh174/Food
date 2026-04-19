package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantDetailVO implements Serializable {
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String logoUrl;
    private Integer status;
    private String contactName;
    private String contactPhone;
    private String inviteCode;
    private Integer apiQuota;
    private Integer apiUsed;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
