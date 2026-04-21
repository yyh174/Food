package com.xl.can.service;

import com.xl.can.common.Result;
import com.xl.can.entity.ReplyTemplate;

import java.util.List;

public interface ReplyTemplateService {

    Result<List<ReplyTemplate>> getTemplateList(Long tenantId);
}
