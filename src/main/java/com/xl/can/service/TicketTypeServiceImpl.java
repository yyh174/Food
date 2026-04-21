package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.context.UserContext;
import com.xl.can.dto.TicketTypeCreateDTO;
import com.xl.can.entity.Shop;
import com.xl.can.entity.TicketType;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.TicketTypeMapper;
import com.xl.can.vo.TicketTypeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketTypeServiceImpl implements TicketTypeService {

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public Result<List<TicketTypeVO>> list(String type) {
        Long tenantId = UserContext.getTenantId();

        LambdaQueryWrapper<TicketType> wrapper = new LambdaQueryWrapper<>();

        if (tenantId != null) {
            wrapper.eq(TicketType::getTenantId, tenantId).or().isNull(TicketType::getTenantId);
        }

        wrapper.eq(TicketType::getStatus, 1);

        if ("system".equals(type)) {
            wrapper.eq(TicketType::getType, "system");
        } else if ("custom".equals(type)) {
            wrapper.eq(TicketType::getType, "custom");
        }

        wrapper.orderByAsc(TicketType::getSortOrder);

        List<TicketType> types = ticketTypeMapper.selectList(wrapper);

        List<TicketTypeVO> voList = types.stream().map(t -> {
            TicketTypeVO vo = new TicketTypeVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setType(t.getType());
            vo.setIsDefault(t.getIsDefault());
            vo.setSupportReview(t.getSupportReview());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<TicketTypeVO> create(TicketTypeCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        String roleCode = UserContext.getRoleCode();

        if (!"tenant_admin".equals(roleCode)) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可创建工单类型");
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "类型名称不能为空");
        }
        if (dto.getName().length() > 20) {
            return Result.error(ResultCode.BAD_REQUEST, "类型名称最多20字");
        }

        LambdaQueryWrapper<TicketType> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(TicketType::getTenantId, tenantId)
                .eq(TicketType::getName, dto.getName().trim());
        if (ticketTypeMapper.selectCount(checkWrapper) > 0) {
            return Result.error(ResultCode.CONFLICT, "该类型名称已存在");
        }

        TicketType ticketType = new TicketType();
        ticketType.setTenantId(tenantId);
        ticketType.setName(dto.getName().trim());
        ticketType.setType("custom");
        ticketType.setIsDefault(false);
        ticketType.setSupportReview(false);
        ticketType.setSortOrder(0);
        ticketType.setStatus(1);

        ticketTypeMapper.insert(ticketType);

        TicketTypeVO vo = new TicketTypeVO();
        vo.setId(ticketType.getId());
        vo.setName(ticketType.getName());
        vo.setType(ticketType.getType());
        vo.setIsDefault(ticketType.getIsDefault());
        vo.setSupportReview(ticketType.getSupportReview());

        return Result.success("创建成功", vo);
    }

    @Override
    public Result<Void> delete(Long id) {
        Long tenantId = UserContext.getTenantId();
        String roleCode = UserContext.getRoleCode();

        if (!"tenant_admin".equals(roleCode)) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可删除工单类型");
        }

        TicketType ticketType = ticketTypeMapper.selectById(id);
        if (ticketType == null) {
            return Result.error(ResultCode.NOT_FOUND, "类型不存在");
        }

        if (!"custom".equals(ticketType.getType())) {
            return Result.error(ResultCode.BAD_REQUEST, "系统预置类型不可删除");
        }

        if (tenantId != null && !tenantId.equals(ticketType.getTenantId())) {
            return Result.error(ResultCode.FORBIDDEN, "无权删除该类型");
        }

        ticketTypeMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
