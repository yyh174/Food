package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.dto.TenantCreateRequest;
import com.xl.can.dto.TenantUpdateRequest;
import com.xl.can.service.TenantService;
import com.xl.can.vo.ShopListVO;
import com.xl.can.vo.TenantCreateVO;
import com.xl.can.vo.TenantDetailVO;
import com.xl.can.vo.TenantListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    /**
     * 租户列表
     */
    @GetMapping
    public Result<PageResult<TenantListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        checkSuperAdmin();
        return tenantService.pageList(page, pageSize, keyword, status);
    }

    /**
     * 租户详情
     */
    @GetMapping("/{id}")
    public Result<TenantDetailVO> detail(@PathVariable Long id) {
        checkSuperAdmin();
        return tenantService.getById(id);
    }

    /**
     * 创建租户
     */
    @PostMapping
    public Result<TenantCreateVO> create(@RequestBody TenantCreateRequest request) {
        checkSuperAdmin();
        return tenantService.create(request);
    }

    /**
     * 编辑租户
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TenantUpdateRequest request) {
        checkSuperAdmin();
        return tenantService.update(id, request);
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkSuperAdmin();
        return tenantService.delete(id);
    }

    /**
     * 重置邀请码
     */
    @PostMapping("/{id}/invite-code/regenerate")
    public Result<String> regenerateInviteCode(@PathVariable Long id) {
        checkSuperAdmin();
        return tenantService.regenerateInviteCode(id);
    }

    /**
     * 租户门店列表
     */
    @GetMapping("/{id}/shops")
    public Result<PageResult<ShopListVO>> shopList(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        checkSuperAdmin();
        return tenantService.getShopList(id, page, pageSize, keyword, status);
    }

    /**
     * 检查是否为超级管理员
     */
    private void checkSuperAdmin() {
        String roleCode = UserContext.getUser().getRoleCode();
        if (!"super_admin".equals(roleCode)) {
            throw new RuntimeException("权限不足，仅平台管理员可访问");
        }
    }
}
