package com.xl.can.controller;

import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.entity.ReplyTemplate;
import com.xl.can.service.ReplyTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reply-templates")
public class ReplyTemplateController {

    @Autowired
    private ReplyTemplateService replyTemplateService;

    @GetMapping
    public Result<List<ReplyTemplate>> list() {
        Long tenantId = UserContext.getUser().getTenantId();
        return replyTemplateService.getTemplateList(tenantId);
    }
}
