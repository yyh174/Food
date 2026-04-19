package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantListVO implements Serializable {
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
    private Integer shopCount;
    private Integer userCount;
    private LocalDateTime createdAt;
}
