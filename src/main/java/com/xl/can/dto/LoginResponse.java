package com.xl.can.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private Long tenantId;
    private String tenantName;
    private String roleCode;
    private String roleName;
    private Long shopId;
    private String shopName;
}
