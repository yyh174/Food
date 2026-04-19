package com.xl.can.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPasswordResetVO implements Serializable {
    private String newPassword;
}
