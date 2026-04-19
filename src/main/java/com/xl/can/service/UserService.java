package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.dto.*;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysRole;
import com.xl.can.entity.SysUser;
import com.xl.can.entity.Tenant;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.SysRoleMapper;
import com.xl.can.mapper.SysUserMapper;
import com.xl.can.mapper.TenantMapper;
import com.xl.can.utils.PasswordUtils;
import com.xl.can.vo.UserCreateVO;
import com.xl.can.vo.UserDetailVO;
import com.xl.can.vo.UserListVO;
import com.xl.can.vo.UserPasswordResetVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private TenantMapper tenantMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 用户列表（分页）
     */
    public Result<PageResult<UserListVO>> pageList(Long tenantId, Integer page, Integer pageSize,
                                                    String keyword, String roleCode, Integer status) {
        Page<SysUser> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getTenantId, tenantId);

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (roleCode != null && !roleCode.isEmpty()) {
            queryWrapper.eq(SysUser::getRoleCode, roleCode);
        }
        if (status != null) {
            queryWrapper.eq(SysUser::getStatus, status);
        }
        queryWrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> result = sysUserMapper.selectPage(pageParam, queryWrapper);

        // 获取角色映射
        Map<String, String> roleNameMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getRoleName, (a, b) -> a));

        // 获取关联门店名称
        Map<Long, String> shopNameMap = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>().eq(Shop::getTenantId, tenantId)
        ).stream().collect(Collectors.toMap(Shop::getId, Shop::getShopName, (a, b) -> a));

        List<UserListVO> voList = result.getRecords().stream().map(user -> {
            UserListVO vo = new UserListVO();
            BeanUtils.copyProperties(user, vo);
            vo.setRoleName(roleNameMap.getOrDefault(user.getRoleCode(), user.getRoleCode()));
            vo.setShopName(user.getShopId() != null ? shopNameMap.get(user.getShopId()) : null);

            if (user.getLastLoginTime() != null) {
                vo.setLastLoginTime(user.getLastLoginTime().format(DATE_TIME_FORMATTER));
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<UserListVO> pageResult = PageResult.of(voList, result.getTotal(), page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 用户详情
     */
    public Result<UserDetailVO> getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserDetailVO vo = new UserDetailVO();
        BeanUtils.copyProperties(user, vo);

        // 角色名称
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, user.getRoleCode())
        );
        if (role != null) {
            vo.setRoleName(role.getRoleName());
        }

        // 门店名称
        if (user.getShopId() != null) {
            Shop shop = shopMapper.selectById(user.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getShopName());
            }
        }

        // 租户名称
        if (user.getTenantId() != null) {
            Tenant tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant != null) {
                vo.setTenantName(tenant.getTenantName());
            }
        }

        if (user.getLastLoginTime() != null) {
            vo.setLastLoginTime(user.getLastLoginTime().format(DATE_TIME_FORMATTER));
        }

        return Result.success(vo);
    }

    /**
     * 创建用户
     */
    @Transactional
    public Result<UserCreateVO> create(Long tenantId, UserCreateRequest request) {
        // 检查用户名是否已存在
        Long existCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getUsername, request.getUsername())
        );
        if (existCount > 0) {
            return Result.error(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        // 验证角色
        if (!"tenant_admin".equals(request.getRoleCode()) && !"shop_owner".equals(request.getRoleCode())) {
            return Result.error(ResultCode.BAD_REQUEST, "角色编码不正确");
        }

        // 验证门店（店长必须关联一个门店）
        if ("shop_owner".equals(request.getRoleCode())) {
            if (request.getShopId() == null) {
                return Result.error(ResultCode.BAD_REQUEST, "店长必须关联一个门店");
            }
            Shop shop = shopMapper.selectById(request.getShopId());
            if (shop == null || !shop.getTenantId().equals(tenantId)) {
                return Result.error(ResultCode.BAD_REQUEST, "门店不存在或不属于当前租户");
            }
            // 检查该门店是否已被其他店长分配
            if (isShopAssigned(tenantId, request.getShopId())) {
                return Result.error(ResultCode.BAD_REQUEST, "该门店已被其他店长分配");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRoleCode(request.getRoleCode());
        user.setTenantId(tenantId);
        user.setShopId(request.getShopId());
        user.setStatus(1);

        String password = request.getPassword() != null ? request.getPassword() : "123456";
        user.setPassword(PasswordUtils.encode(password));

        sysUserMapper.insert(user);

        UserCreateVO vo = new UserCreateVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleCode(user.getRoleCode());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());

        return Result.success("创建成功", vo);
    }

    /**
     * 编辑用户
     */
    @Transactional
    public Result<Void> update(Long id, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 验证角色
        if (!"tenant_admin".equals(request.getRoleCode()) && !"shop_owner".equals(request.getRoleCode())) {
            return Result.error(ResultCode.BAD_REQUEST, "角色编码不正确");
        }

        // 验证门店
        if ("shop_owner".equals(request.getRoleCode())) {
            if (request.getShopId() == null) {
                return Result.error(ResultCode.BAD_REQUEST, "店长必须关联一个门店");
            }
            Shop shop = shopMapper.selectById(request.getShopId());
            if (shop == null || !shop.getTenantId().equals(user.getTenantId())) {
                return Result.error(ResultCode.BAD_REQUEST, "门店不存在或不属于当前租户");
            }
            // 检查该门店是否已被其他店长分配（排除当前用户）
            if (isShopAssignedByOther(user.getTenantId(), request.getShopId(), id)) {
                return Result.error(ResultCode.BAD_REQUEST, "该门店已被其他店长分配");
            }
        }

        Integer oldStatus = user.getStatus();

        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRoleCode(request.getRoleCode());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setShopId(request.getShopId());

        sysUserMapper.updateById(user);

        // 禁用时清空门店关联
        if (oldStatus == 1 && request.getStatus() != null && request.getStatus() == 0) {
            user.setShopId(null);
            sysUserMapper.updateById(user);
        }

        return Result.success("更新成功", null);
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional
    public Result<Void> delete(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setShopId(null);
        sysUserMapper.updateById(user);
        sysUserMapper.deleteById(id);

        return Result.success("删除成功", null);
    }

    /**
     * 重置用户密码
     */
    public Result<UserPasswordResetVO> resetPassword(Long id, UserPasswordResetRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        String newPassword = request.getNewPassword() != null ? request.getNewPassword() : "123456";
        user.setPassword(PasswordUtils.encode(newPassword));
        sysUserMapper.updateById(user);

        UserPasswordResetVO vo = new UserPasswordResetVO();
        vo.setNewPassword(newPassword);

        return Result.success("密码重置成功", vo);
    }

    /**
     * 修改当前用户密码
     */
    public Result<Void> changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "原密码不正确");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);

        return Result.success("密码修改成功", null);
    }

    /**
     * 修改用户门店关联
     */
    @Transactional
    public Result<Void> updateUserShops(Long id, UserShopUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (!"shop_owner".equals(user.getRoleCode())) {
            return Result.error(ResultCode.BAD_REQUEST, "只有门店店长可以修改门店关联");
        }

        if (request.getShopId() != null) {
            Shop shop = shopMapper.selectById(request.getShopId());
            if (shop == null || !shop.getTenantId().equals(user.getTenantId())) {
                return Result.error(ResultCode.BAD_REQUEST, "门店不存在或不属于当前租户");
            }
        }

        user.setShopId(request.getShopId());
        sysUserMapper.updateById(user);

        return Result.success("门店关联已更新", null);
    }

    /**
     * 更新当前用户信息（只能修改基本信息）
     */
    public Result<Void> updateCurrentUser(Long userId, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        sysUserMapper.updateById(user);

        return Result.success("更新成功", null);
    }

    /**
     * 检查门店是否已被其他店长分配
     */
    private boolean isShopAssigned(Long tenantId, Long shopId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getRoleCode, "shop_owner")
                .eq(SysUser::getShopId, shopId);
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查门店是否已被其他店长分配（排除指定用户）
     */
    private boolean isShopAssignedByOther(Long tenantId, Long shopId, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getRoleCode, "shop_owner")
                .ne(SysUser::getId, excludeUserId)
                .eq(SysUser::getShopId, shopId);
        return sysUserMapper.selectCount(wrapper) > 0;
    }
}
