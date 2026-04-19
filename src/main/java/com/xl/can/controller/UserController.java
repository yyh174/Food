package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.dto.*;
import com.xl.can.service.UserService;
import com.xl.can.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户列表（分页）
     */
    @GetMapping
    public Result<PageResult<UserListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return userService.pageList(tenantId, page, pageSize, keyword, roleCode, status);
    }

    /**
     * 用户详情
     */
    @GetMapping("/{id}")
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        checkTenantAdmin();
        return userService.getById(id);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserCreateVO> create(@RequestBody UserCreateRequest request) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return userService.create(tenantId, request);
    }

    /**
     * 编辑用户
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        checkTenantAdmin();
        return userService.update(id, request);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkTenantAdmin();
        return userService.delete(id);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/password/reset")
    public Result<UserPasswordResetVO> resetPassword(@PathVariable Long id,
                                                     @RequestBody(required = false) UserPasswordResetRequest request) {
        checkTenantAdmin();
        if (request == null) {
            request = new UserPasswordResetRequest();
        }
        return userService.resetPassword(id, request);
    }

    /**
     * 修改当前用户密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        return userService.changePassword(userId, request);
    }

    /**
     * 修改用户门店关联
     */
    @PutMapping("/{id}/shops")
    public Result<Void> updateUserShops(@PathVariable Long id, @RequestBody UserShopUpdateRequest request) {
        checkTenantAdmin();
        return userService.updateUserShops(id, request);
    }

    /**
     * 检查是否为租户管理员
     */
    private void checkTenantAdmin() {
        String roleCode = UserContext.getUser().getRoleCode();
        if (!"tenant_admin".equals(roleCode)) {
            throw new RuntimeException("权限不足，仅租户管理员可访问");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserDetailVO> getCurrentUser() {
        Long userId = UserContext.getUserId();
        return userService.getById(userId);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public Result<Void> updateCurrentUser(@RequestBody UserUpdateRequest request) {
        Long userId = UserContext.getUserId();
        return userService.updateCurrentUser(userId, request);
    }
}
