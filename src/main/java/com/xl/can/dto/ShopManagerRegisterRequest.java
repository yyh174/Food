package com.xl.can.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopManagerRegisterRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
