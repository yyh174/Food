package com.xl.can.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantCreateRequest implements Serializable {
    private String tenantName;
    private String logoUrl;
    private String contactName;
    private String contactPhone;
    private Integer apiQuota;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
