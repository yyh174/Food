package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.dto.TenantCreateRequest;
import com.xl.can.dto.TenantUpdateRequest;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysUser;
import com.xl.can.entity.Tenant;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.SysUserMapper;
import com.xl.can.mapper.TenantMapper;
import com.xl.can.utils.PasswordUtils;
import com.xl.can.vo.ShopListVO;
import com.xl.can.vo.TenantCreateVO;
import com.xl.can.vo.TenantDetailVO;
import com.xl.can.vo.TenantListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantService {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ShopMapper shopMapper;

    public Result<PageResult<TenantListVO>> pageList(Integer page, Integer pageSize, String keyword, Integer status) {
        Page<Tenant> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Tenant::getTenantCode, keyword)
                    .or().like(Tenant::getTenantName, keyword));
        }
        if (status != null) {
            queryWrapper.eq(Tenant::getStatus, status);
        }
        queryWrapper.orderByDesc(Tenant::getCreatedAt);

        Page<Tenant> result = tenantMapper.selectPage(pageParam, queryWrapper);

        List<TenantListVO> voList = result.getRecords().stream().map(tenant -> {
            TenantListVO vo = new TenantListVO();
            BeanUtils.copyProperties(tenant, vo);

            // 统计门店数量
            LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
            shopWrapper.eq(Shop::getTenantId, tenant.getId());
            Long shopCount = shopMapper.selectCount(shopWrapper);
            vo.setShopCount(shopCount.intValue());

            // 统计用户数量
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getTenantId, tenant.getId());
            Long userCount = sysUserMapper.selectCount(userWrapper);
            vo.setUserCount(userCount.intValue());

            return vo;
        }).collect(Collectors.toList());

        PageResult<TenantListVO> pageResult = PageResult.of(voList, result.getTotal(), page, pageSize);
        return Result.success(pageResult);
    }

    public Result<TenantDetailVO> getById(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND, "租户不存在");
        }

        TenantDetailVO vo = new TenantDetailVO();
        BeanUtils.copyProperties(tenant, vo);
        return Result.success(vo);
    }

    @Transactional
    public Result<TenantCreateVO> create(TenantCreateRequest request) {
        // 检查租户名称是否已存在
        Long existCount = tenantMapper.selectCount(
            new LambdaQueryWrapper<Tenant>().eq(Tenant::getTenantName, request.getTenantName())
        );
        if (existCount > 0) {
            return Result.error(ResultCode.BAD_REQUEST, "租户名称已存在");
        }

        // 生成租户编码
        String tenantCode = generateTenantCode();

        // 生成邀请码
        String inviteCode = generateInviteCode();

        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(request.getTenantName());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setInviteCode(inviteCode);
        tenant.setApiQuota(request.getApiQuota() != null ? request.getApiQuota() : 1000);
        tenant.setApiUsed(0);
        tenant.setExpireTime(request.getExpireTime() != null ? request.getExpireTime() : LocalDateTime.now().plusYears(1));
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        // 创建默认管理员账号
        String adminUsername = tenantCode + "_admin";
        String randomPassword = PasswordUtils.generateRandomPassword(9);
        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(PasswordUtils.encode(randomPassword));
        admin.setTenantId(tenant.getId());
        admin.setRoleCode("tenant_admin");
        admin.setStatus(1);
        sysUserMapper.insert(admin);

        TenantCreateVO vo = new TenantCreateVO();
        vo.setId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getTenantName());
        vo.setInviteCode(tenant.getInviteCode());
        vo.setStatus(tenant.getStatus());

        return Result.success("创建成功", vo);
    }

    @Transactional
    public Result<Void> update(Long id, TenantUpdateRequest request) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND, "租户不存在");
        }

        // 检查租户名称是否被其他租户使用
        Long existCount = tenantMapper.selectCount(
            new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantName, request.getTenantName())
                .ne(Tenant::getId, id)
        );
        if (existCount > 0) {
            return Result.error(ResultCode.BAD_REQUEST, "租户名称已存在");
        }

        // 记录原状态
        Integer oldStatus = tenant.getStatus();

        // 更新租户信息
        tenant.setTenantName(request.getTenantName());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        if (request.getApiQuota() != null) {
            tenant.setApiQuota(request.getApiQuota());
        }
        if (request.getExpireTime() != null) {
            tenant.setExpireTime(request.getExpireTime());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        tenantMapper.updateById(tenant);

        // 如果状态改为禁用，联动禁用用户、门店休息
        if (oldStatus == 1 && request.getStatus() != null && request.getStatus() == 0) {
            // 禁用该租户下所有用户
            LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
            userWrapper.eq(SysUser::getTenantId, id)
                    .set(SysUser::getStatus, 0);
            sysUserMapper.update(null, userWrapper);

            // 设置该租户下所有门店为休息中
            LambdaUpdateWrapper<Shop> shopWrapper = new LambdaUpdateWrapper<>();
            shopWrapper.eq(Shop::getTenantId, id)
                    .set(Shop::getStatus, 2);
            shopMapper.update(null, shopWrapper);
        }

        return Result.success("更新成功", null);
    }

    @Transactional
    public Result<Void> delete(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND, "租户不存在");
        }

        // 软删除该租户下的所有用户
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, id);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);
        for (SysUser user : users) {
            sysUserMapper.deleteById(user.getId());
        }

        // 软删除该租户下的所有门店
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, id);
        List<Shop> shops = shopMapper.selectList(shopWrapper);
        for (Shop shop : shops) {
            shopMapper.deleteById(shop.getId());
        }

        // 软删除租户
        tenantMapper.deleteById(id);

        return Result.success("删除成功", null);
    }

    @Transactional
    public Result<String> regenerateInviteCode(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND, "租户不存在");
        }

        String newInviteCode = generateInviteCode();
        tenant.setInviteCode(newInviteCode);
        tenantMapper.updateById(tenant);

        return Result.success("重置成功", newInviteCode);
    }

    public Result<PageResult<ShopListVO>> getShopList(Long tenantId, Integer page, Integer pageSize, String keyword, Integer status) {
        // 验证租户是否存在
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND, "租户不存在");
        }

        Page<Shop> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getTenantId, tenantId);

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Shop::getShopCode, keyword)
                    .or().like(Shop::getShopName, keyword));
        }
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);

        Page<Shop> result = shopMapper.selectPage(pageParam, queryWrapper);

        List<ShopListVO> voList = result.getRecords().stream().map(shop -> {
            ShopListVO vo = new ShopListVO();
            BeanUtils.copyProperties(shop, vo);
            return vo;
        }).collect(Collectors.toList());

        PageResult<ShopListVO> pageResult = PageResult.of(voList, result.getTotal(), page, pageSize);
        return Result.success(pageResult);
    }

    private String generateTenantCode() {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper();
        wrapper.likeRight(Tenant::getTenantCode, "TN")
                .orderByDesc(Tenant::getTenantCode)
                .last("LIMIT 1");
        Tenant lastTenant = tenantMapper.selectOne(wrapper);

        if (lastTenant == null) {
            return "TN01";
        }

        String lastCode = lastTenant.getTenantCode();
        String numStr = lastCode.substring(2);
        int num = Integer.parseInt(numStr);
        return "TN" + String.format("%02d", num + 1);
    }

    private String generateInviteCode() {
        return "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
