package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPasswordResetRequest implements Serializable {
    private String newPassword;
}
