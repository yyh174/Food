package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.dto.*;
import com.xl.can.entity.InviteCodeRecord;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysRole;
import com.xl.can.entity.SysUser;
import com.xl.can.entity.Tenant;
import com.xl.can.mapper.InviteCodeRecordMapper;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.SysRoleMapper;
import com.xl.can.mapper.SysUserMapper;
import com.xl.can.mapper.TenantMapper;
import com.xl.can.utils.JwtUtils;
import com.xl.can.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private InviteCodeRecordMapper inviteCodeRecordMapper;

    @Autowired
    private JwtUtils jwtUtils;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Result<LoginResponse> login(LoginRequest request) {
        String username = request.getUsername();
        String rawPassword = request.getPassword();

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        if (user == null) {
            return Result.error(ResultCode.BAD_REQUEST, "用户不存在");
        }

        if (!PasswordUtils.matches(rawPassword, user.getPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            return Result.error(ResultCode.FORBIDDEN, "账号不可用");
        }

        // 店长必须有关联的门店才能登录
        if ("shop_owner".equals(user.getRoleCode()) && user.getShopId() == null) {
            return Result.error(ResultCode.FORBIDDEN, "账号未分配门店，请联系租户管理员");
        }

        Tenant tenant = null;
        if (user.getTenantId() != null) {
            tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant != null && tenant.getStatus() == 0) {
                return Result.error(ResultCode.FORBIDDEN, "账号不可用");
            }
            if (tenant != null && tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
                return Result.error(ResultCode.FORBIDDEN, "账号不可用");
            }
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now());
        sysUserMapper.update(null, updateWrapper);

        String roleName = getRoleName(user.getRoleCode());

        Long shopId = null;
        String shopName = null;
        if (user.getShopId() != null) {
            shopId = user.getShopId();
            Shop shop = shopMapper.selectById(shopId);
            if (shop != null) {
                shopName = shop.getShopName();
            }
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRoleCode(), user.getTenantId(), user.getShopId());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .tenantId(user.getTenantId())
                .tenantName(tenant != null ? tenant.getTenantName() : null)
                .roleCode(user.getRoleCode())
                .roleName(roleName)
                .shopId(shopId)
                .shopName(shopName)
                .build();

        return Result.success("登录成功", response);
    }

    @Transactional
    public Result<UserResponse> register(RegisterRequest request) {
        LambdaQueryWrapper<SysUser> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser existingUser = sysUserMapper.selectOne(checkWrapper);
        if (existingUser != null) {
            return Result.error(ResultCode.CONFLICT, "该用户名已存在");
        }

        String encryptedPassword = PasswordUtils.encode(request.getPassword());

        Tenant tenant = new Tenant();
        tenant.setTenantCode("T" + System.currentTimeMillis());
        tenant.setTenantName(request.getUsername() + "的租户");
        tenant.setInviteCode(generateInviteCode());
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(encryptedPassword);
        user.setTenantId(tenant.getId());
        user.setRoleCode("tenant_admin");
        user.setStatus(1);
        sysUserMapper.insert(user);

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .tenantId(tenant.getId())
                .roleCode("tenant_admin")
                .build();

        return Result.success("用户创建成功", response);
    }

    @Transactional
    public Result<LoginResponse> shopManagerRegister(ShopManagerRegisterRequest request) {
        LambdaQueryWrapper<InviteCodeRecord> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(InviteCodeRecord::getInviteCode, request.getInviteCode())
                .eq(InviteCodeRecord::getStatus, "unused");
        InviteCodeRecord inviteCode = inviteCodeRecordMapper.selectOne(codeWrapper);

        if (inviteCode == null) {
            return Result.error(ResultCode.BAD_REQUEST, "邀请码无效或已过期");
        }

        if (inviteCode.getExpireTime().isBefore(LocalDateTime.now())) {
            return Result.error(ResultCode.BAD_REQUEST, "邀请码已过期");
        }

        // 检查是否有同名软删除用户，如果有则先物理删除（释放唯一索引）
        SysUser deletedUser = sysUserMapper.selectDeletedByTenantAndUsername(inviteCode.getTenantId(), request.getUsername());
        if (deletedUser != null) {
            sysUserMapper.physicalDeleteById(deletedUser.getId());
        }

        String encryptedPassword = PasswordUtils.encode(request.getPassword());

        // 创建新用户（店长角色，门店待分配）
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(encryptedPassword);
        user.setTenantId(inviteCode.getTenantId());
        user.setRoleCode("shop_owner");
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 更新邀请码状态
        inviteCode.setStatus("used");
        inviteCode.setUsedTime(LocalDateTime.now());
        inviteCode.setUsedUserId(user.getId());
        inviteCodeRecordMapper.updateById(inviteCode);

        Tenant tenant = tenantMapper.selectById(inviteCode.getTenantId());

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRoleCode(), user.getTenantId(), null);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .tenantId(user.getTenantId())
                .tenantName(tenant != null ? tenant.getTenantName() : null)
                .roleCode("shop_owner")
                .roleName("门店店长")
                .build();

        return Result.success("注册成功，请联系租户管理员分配门店", response);
    }

    @Transactional
    public Result<Void> resetPassword(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        String newPassword = PasswordUtils.generateRandomPassword(9);

        user.setPassword(PasswordUtils.encode(newPassword));
        sysUserMapper.updateById(user);

        return Result.success("密码重置成功，新密码为：" + newPassword, null);
    }

    @Transactional
    public Result<Void> changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error(ResultCode.BAD_REQUEST, "旧密码错误");
        }

        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);

        return Result.success("密码修改成功", null);
    }

    private String getRoleName(String roleCode) {
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getRoleCode, roleCode);
        SysRole role = sysRoleMapper.selectOne(roleWrapper);
        return role != null ? role.getRoleName() : roleCode;
    }

    private String generateInviteCode() {
        return "INV" + System.currentTimeMillis();
    }

    public SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    public SysUser getUserByUsername(String username, Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (tenantId != null) {
            wrapper.eq(SysUser::getTenantId, tenantId);
        }
        return sysUserMapper.selectOne(wrapper);
    }
}
