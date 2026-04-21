package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.common.Result;
import com.xl.can.entity.ReplyTemplate;
import com.xl.can.mapper.ReplyTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReplyTemplateServiceImpl implements ReplyTemplateService {

    @Autowired
    private ReplyTemplateMapper replyTemplateMapper;

    @Override
    public Result<List<ReplyTemplate>> getTemplateList(Long tenantId) {
        LambdaQueryWrapper<ReplyTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReplyTemplate::getTenantId, tenantId)
                .eq(ReplyTemplate::getStatus, 1)
                .orderByDesc(ReplyTemplate::getSortOrder);

        List<ReplyTemplate> templates = replyTemplateMapper.selectList(queryWrapper);
        return Result.success(templates);
    }
}
