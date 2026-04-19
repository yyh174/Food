package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.dto.ShopCreateRequest;
import com.xl.can.dto.ShopStatusRequest;
import com.xl.can.dto.ShopUpdateRequest;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysUser;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.SysUserMapper;
import com.xl.can.vo.ShopCreateVO;
import com.xl.can.vo.ShopDetailVO;
import com.xl.can.vo.ShopListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShopService {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 门店列表（分页）
     */
    public Result<PageResult<ShopListVO>> pageList(Long tenantId, Integer page, Integer pageSize, 
                                                   String keyword, Integer status) {
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
        }).collect(java.util.stream.Collectors.toList());

        PageResult<ShopListVO> pageResult = PageResult.of(voList, result.getTotal(), page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 门店详情
     */
    public Result<ShopDetailVO> getById(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.error(ResultCode.NOT_FOUND, "门店不存在");
        }

        ShopDetailVO vo = new ShopDetailVO();
        BeanUtils.copyProperties(shop, vo);
        return Result.success(vo);
    }

    /**
     * 创建门店
     */
    @Transactional
    public Result<ShopCreateVO> create(Long tenantId, ShopCreateRequest request) {
        // 验证 meituanId 和 eleId 至少填写一个
        if ((request.getMeituanId() == null || request.getMeituanId().isEmpty()) 
                && (request.getEleId() == null || request.getEleId().isEmpty())) {
            return Result.error(ResultCode.BAD_REQUEST, "美团门店ID和饿了么门店ID至少填写一个");
        }

        // 检查美团ID是否已被其他门店使用（全局唯一）
        if (request.getMeituanId() != null && !request.getMeituanId().isEmpty()) {
            Long existCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>().eq(Shop::getMeituanId, request.getMeituanId())
            );
            if (existCount > 0) {
                return Result.error(ResultCode.BAD_REQUEST, "该美团门店ID已被其他门店绑定");
            }
        }

        // 检查饿了么ID是否已被其他门店使用（全局唯一）
        if (request.getEleId() != null && !request.getEleId().isEmpty()) {
            Long existCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>().eq(Shop::getEleId, request.getEleId())
            );
            if (existCount > 0) {
                return Result.error(ResultCode.BAD_REQUEST, "该饿了么门店ID已被其他门店绑定");
            }
        }

        // 生成门店编码
        String shopCode = generateShopCode(tenantId);

        Shop shop = new Shop();
        shop.setTenantId(tenantId);
        shop.setShopCode(shopCode);
        shop.setShopName(request.getShopName());
        shop.setProvince(request.getProvince());
        shop.setCity(request.getCity());
        shop.setDistrict(request.getDistrict());
        shop.setAddress(request.getAddress());
        shop.setManagerName(request.getManagerName());
        shop.setManagerPhone(request.getManagerPhone());
        shop.setMeituanId(request.getMeituanId());
        shop.setEleId(request.getEleId());
        shop.setStatus(1); // 默认营业中
        shopMapper.insert(shop);

        ShopCreateVO vo = new ShopCreateVO();
        vo.setId(shop.getId());
        vo.setShopCode(shop.getShopCode());
        vo.setShopName(shop.getShopName());
        vo.setStatus(shop.getStatus());
        vo.setCreatedAt(shop.getCreatedAt());

        return Result.success("创建成功", vo);
    }

    /**
     * 编辑门店
     */
    @Transactional
    public Result<Void> update(Long tenantId, Long id, ShopUpdateRequest request) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.error(ResultCode.NOT_FOUND, "门店不存在");
        }

        // 记录原状态
        Integer oldStatus = shop.getStatus();

        // 检查美团ID是否已被其他门店使用（排除自己，全局唯一）
        if (request.getMeituanId() != null && !request.getMeituanId().isEmpty()) {
            Long existCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getMeituanId, request.getMeituanId())
                    .ne(Shop::getId, id)
            );
            if (existCount > 0) {
                return Result.error(ResultCode.BAD_REQUEST, "该美团门店ID已被其他门店绑定");
            }
        }

        // 检查饿了么ID是否已被其他门店使用（排除自己，全局唯一）
        if (request.getEleId() != null && !request.getEleId().isEmpty()) {
            Long existCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getEleId, request.getEleId())
                    .ne(Shop::getId, id)
            );
            if (existCount > 0) {
                return Result.error(ResultCode.BAD_REQUEST, "该饿了么门店ID已被其他门店绑定");
            }
        }

        // 更新门店信息（shopCode 不可修改）
        shop.setShopName(request.getShopName());
        shop.setProvince(request.getProvince());
        shop.setCity(request.getCity());
        shop.setDistrict(request.getDistrict());
        shop.setAddress(request.getAddress());
        shop.setManagerName(request.getManagerName());
        shop.setManagerPhone(request.getManagerPhone());
        shop.setMeituanId(request.getMeituanId());
        shop.setEleId(request.getEleId());
        if (request.getStatus() != null) {
            shop.setStatus(request.getStatus());
        }
        shopMapper.updateById(shop);

        // 营业中 → 休息中：禁用店长，不释放关联
        if (oldStatus == 1 && request.getStatus() != null && request.getStatus() == 2) {
            disableShopOwner(tenantId, id);
        }

        // 变为已停业：释放店长关联
        if (request.getStatus() != null && request.getStatus() == 0 && oldStatus != 0) {
            releaseShopOwner(tenantId, id);
        }

        // 停业 → 营业中：释放原店长关联（允许分配给新店长）
        if (oldStatus == 0 && request.getStatus() != null && request.getStatus() == 1) {
            releaseShopOwner(tenantId, id);
        }

        return Result.success("更新成功", null);
    }

    /**
     * 删除门店（软删除）
     */
    @Transactional
    public Result<Void> delete(Long tenantId, Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.error(ResultCode.NOT_FOUND, "门店不存在");
        }

        // 释放店长关联
        releaseShopOwner(tenantId, id);

        // 软删除（使用 deleteById 会自动设置 deleted_at=1）
        shopMapper.deleteById(id);

        return Result.success("删除成功", null);
    }

    /**
     * 修改营业状态（仅营业中/休息中切换）
     */
    public Result<Void> updateStatus(Long tenantId, Long id, ShopStatusRequest request) {
        // 验证门店存在
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return Result.error(ResultCode.NOT_FOUND, "门店不存在");
        }

        Integer oldStatus = shop.getStatus();

        // 只能切换到 1（营业中）或 2（休息中）
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            return Result.error(ResultCode.BAD_REQUEST, "营业状态只能切换为营业中或休息中");
        }

        shop.setStatus(request.getStatus());
        shopMapper.updateById(shop);

        // 营业中 → 休息中：禁用店长，不释放关联
        if (oldStatus == 1 && request.getStatus() == 2) {
            disableShopOwner(tenantId, id);
        }

        return Result.success("状态更新成功", null);
    }

    /**
     * 释放门店下所有店长的门店关联并禁用用户（仅处理指定租户）
     */
    private void releaseShopOwner(Long tenantId, Long shopId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getRoleCode, "shop_owner")
                .eq(SysUser::getShopId, shopId);
        List<SysUser> shopOwners = sysUserMapper.selectList(queryWrapper);

        for (SysUser user : shopOwners) {
            // 使用 UpdateWrapper 强制设置 shop_id 为 null
            LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysUser::getId, user.getId())
                    .set(SysUser::getShopId, null)
                    .set(SysUser::getStatus, 0);
            sysUserMapper.update(null, updateWrapper);
        }
    }

    /**
     * 生成门店编码（SHOP + 6位序号，按租户生成）
     */
    private String generateShopCode(Long tenantId) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getTenantId, tenantId)
                .likeRight(Shop::getShopCode, "SHOP")
                .orderByDesc(Shop::getShopCode)
                .last("LIMIT 1");
        Shop lastShop = shopMapper.selectOne(wrapper);

        if (lastShop == null) {
            return "SHOP000001";
        }

        String lastCode = lastShop.getShopCode();
        String numStr = lastCode.substring(4);
        int num = Integer.parseInt(numStr);
        return "SHOP" + String.format("%06d", num + 1);
    }

    /**
     * 禁用门店店长（门店变为休息中时调用，不释放关联，仅处理指定租户）
     */
    private void disableShopOwner(Long tenantId, Long shopId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getRoleCode, "shop_owner")
                .eq(SysUser::getShopId, shopId);
        List<SysUser> shopOwners = sysUserMapper.selectList(queryWrapper);

        for (SysUser user : shopOwners) {
            user.setStatus(0);
            sysUserMapper.updateById(user);
        }
    }

    /**
     * 获取可分配的门店列表（排除已被其他店长分配的门店，仅显示营业中门店）
     */
    public Result<List<ShopListVO>> getAvailableShops(Long tenantId) {
        // 获取当前租户的所有营业中门店
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, tenantId)
                .eq(Shop::getStatus, 1);
        List<Shop> openShops = shopMapper.selectList(shopWrapper);

        // 获取已被分配的门店ID
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getRoleCode, "shop_owner")
                .isNotNull(SysUser::getShopId);
        List<SysUser> shopOwners = sysUserMapper.selectList(userWrapper);

        Set<Long> assignedShopIds = shopOwners.stream()
                .map(SysUser::getShopId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 过滤掉已分配的门店
        List<ShopListVO> availableShops = openShops.stream()
                .filter(shop -> !assignedShopIds.contains(shop.getId()))
                .map(shop -> {
                    ShopListVO vo = new ShopListVO();
                    BeanUtils.copyProperties(shop, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.success(availableShops);
    }
}
