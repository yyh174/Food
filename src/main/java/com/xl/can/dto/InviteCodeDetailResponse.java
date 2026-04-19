package com.xl.can.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeDetailResponse {
    private Long id;
    private String inviteCode;
    private String expireTime;
    private String status;
    private String createdAt;
    private String usedTime;
    private Long usedUserId;
    private String usedUsername;
}
