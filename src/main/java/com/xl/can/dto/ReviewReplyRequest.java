package com.xl.can.dto;

import lombok.Data;

@Data
public class ReviewReplyRequest {

    private String content;

    private Boolean useTemplate = false;
}
