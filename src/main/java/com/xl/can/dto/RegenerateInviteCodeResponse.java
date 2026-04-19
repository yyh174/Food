package com.xl.can.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RegenerateInviteCodeResponse implements Serializable {
    private String inviteCode;
}
