package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.dto.ShopCreateRequest;
import com.xl.can.dto.ShopStatusRequest;
import com.xl.can.dto.ShopUpdateRequest;
import com.xl.can.entity.SysUser;
import com.xl.can.service.ShopService;
import com.xl.can.vo.ShopCreateVO;
import com.xl.can.vo.ShopDetailVO;
import com.xl.can.vo.ShopListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 门店列表
     */
    @GetMapping
    public Result<PageResult<ShopListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.pageList(tenantId, page, pageSize, keyword, status);
    }

    /**
     * 门店详情
     */
    @GetMapping("/{id}")
    public Result<ShopDetailVO> detail(@PathVariable Long id) {
        checkTenantAdmin();
        return shopService.getById(id);
    }

    /**
     * 创建门店
     */
    @PostMapping
    public Result<ShopCreateVO> create(@RequestBody ShopCreateRequest request) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.create(tenantId, request);
    }

    /**
     * 编辑门店
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ShopUpdateRequest request) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.update(tenantId, id, request);
    }

    /**
     * 删除门店（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.delete(tenantId, id);
    }

    /**
     * 修改营业状态（店长快速切换）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody ShopStatusRequest request) {
        checkShopOwnerOrAdmin(id);
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.updateStatus(tenantId, id, request);
    }

    /**
     * 获取可分配的门店列表（排除已被其他店长分配的门店）
     * 用于用户管理中选择门店
     */
    @GetMapping("/available")
    public Result<List<ShopListVO>> getAvailableShops() {
        checkTenantAdmin();
        Long tenantId = UserContext.getUser().getTenantId();
        return shopService.getAvailableShops(tenantId);
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
     * 检查是否为店长或租户管理员
     */
    private void checkShopOwnerOrAdmin(Long shopId) {
        SysUser user = UserContext.getUser();
        String roleCode = user.getRoleCode();

        if ("tenant_admin".equals(roleCode)) {
            return;
        }

        if ("shop_owner".equals(roleCode)) {
            if (user.getShopId() != null && user.getShopId().equals(shopId)) {
                return;
            }
            throw new RuntimeException("只能修改自己门店的营业状态");
        }

        throw new RuntimeException("权限不足");
    }
}
