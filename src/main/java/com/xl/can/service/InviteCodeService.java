package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.context.UserContext;
import com.xl.can.dto.InviteCodeDetailResponse;
import com.xl.can.dto.InviteCodeResponse;
import com.xl.can.entity.InviteCodeRecord;
import com.xl.can.entity.SysUser;
import com.xl.can.mapper.InviteCodeRecordMapper;
import com.xl.can.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InviteCodeService {

    @Autowired
    private InviteCodeRecordMapper inviteCodeRecordMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_EXPIRE_DAYS = 30;

    public Result<InviteCodeResponse> generateInviteCode() {
        SysUser currentUser = UserContext.getUser();
        Long tenantId = currentUser.getTenantId();

        String inviteCode = generateUniqueCode();

        InviteCodeRecord record = new InviteCodeRecord();
        record.setTenantId(tenantId);
        record.setInviteCode(inviteCode);
        record.setStatus("unused");
        record.setExpireTime(LocalDateTime.now().plusDays(DEFAULT_EXPIRE_DAYS));
        inviteCodeRecordMapper.insert(record);

        InviteCodeResponse response = InviteCodeResponse.builder()
                .inviteCode(inviteCode)
                .expireTime(record.getExpireTime().format(DATE_TIME_FORMATTER))
                .status("unused")
                .build();

        return Result.success("邀请码生成成功", response);
    }

    public Result<PageResult<InviteCodeDetailResponse>> getInviteCodeList(String status, Integer page, Integer pageSize) {
        SysUser currentUser = UserContext.getUser();
        Long tenantId = currentUser.getTenantId();

        if (page == null) page = 1;
        if (pageSize == null) pageSize = 10;

        LambdaQueryWrapper<InviteCodeRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InviteCodeRecord::getTenantId, tenantId);
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(InviteCodeRecord::getStatus, status);
        }
        queryWrapper.orderByDesc(InviteCodeRecord::getCreatedAt);

        Page<InviteCodeRecord> pageResult = new Page<>(page, pageSize);
        Page<InviteCodeRecord> result = inviteCodeRecordMapper.selectPage(pageResult, queryWrapper);

        List<InviteCodeDetailResponse> records = new ArrayList<>();
        for (InviteCodeRecord record : result.getRecords()) {
            String usedUsername = null;
            if (record.getUsedUserId() != null) {
                SysUser usedUser = sysUserMapper.selectById(record.getUsedUserId());
                if (usedUser != null) {
                    usedUsername = usedUser.getUsername();
                }
            }

            InviteCodeDetailResponse detail = InviteCodeDetailResponse.builder()
                    .id(record.getId())
                    .inviteCode(record.getInviteCode())
                    .expireTime(record.getExpireTime().format(DATE_TIME_FORMATTER))
                    .status(record.getStatus())
                    .createdAt(record.getCreatedAt().format(DATE_TIME_FORMATTER))
                    .usedTime(record.getUsedTime() != null ? record.getUsedTime().format(DATE_TIME_FORMATTER) : null)
                    .usedUserId(record.getUsedUserId())
                    .usedUsername(usedUsername)
                    .build();

            records.add(detail);
        }

        PageResult<InviteCodeDetailResponse> pageResultVO = PageResult.of(records, result.getTotal(), page, pageSize);

        return Result.success(pageResultVO);
    }

    @Transactional
    public Result<Void> invalidateInviteCode(Long id) {
        SysUser currentUser = UserContext.getUser();
        Long tenantId = currentUser.getTenantId();

        InviteCodeRecord record = inviteCodeRecordMapper.selectById(id);
        if (record == null) {
            return Result.error(ResultCode.NOT_FOUND, "邀请码不存在");
        }

        if (!record.getTenantId().equals(tenantId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作此邀请码");
        }

        if ("used".equals(record.getStatus())) {
            return Result.error(ResultCode.BAD_REQUEST, "该邀请码已被使用，无法作废");
        }

        if ("invalidated".equals(record.getStatus())) {
            return Result.error(ResultCode.BAD_REQUEST, "该邀请码已作废");
        }

        record.setStatus("invalidated");
        inviteCodeRecordMapper.updateById(record);

        return Result.success("邀请码已作废", null);
    }

    public Result<InviteCodeDetailResponse> getLatestUnusedInviteCode() {
        SysUser currentUser = UserContext.getUser();
        Long tenantId = currentUser.getTenantId();

        LambdaQueryWrapper<InviteCodeRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InviteCodeRecord::getTenantId, tenantId)
                .eq(InviteCodeRecord::getStatus, "unused")
                .gt(InviteCodeRecord::getExpireTime, LocalDateTime.now())
                .orderByDesc(InviteCodeRecord::getCreatedAt)
                .last("LIMIT 1");

        InviteCodeRecord record = inviteCodeRecordMapper.selectOne(queryWrapper);

        if (record == null) {
            return Result.success("暂无未使用的邀请码", null);
        }

        InviteCodeDetailResponse detail = InviteCodeDetailResponse.builder()
                .id(record.getId())
                .inviteCode(record.getInviteCode())
                .expireTime(record.getExpireTime().format(DATE_TIME_FORMATTER))
                .status(record.getStatus())
                .createdAt(record.getCreatedAt().format(DATE_TIME_FORMATTER))
                .usedTime(null)
                .usedUserId(null)
                .usedUsername(null)
                .build();

        return Result.success(detail);
    }

    @Transactional
    public Result<InviteCodeResponse> regenerateLatestInviteCode() {
        SysUser currentUser = UserContext.getUser();
        Long tenantId = currentUser.getTenantId();

        // 查询当前租户最新的未使用邀请码
        LambdaQueryWrapper<InviteCodeRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InviteCodeRecord::getTenantId, tenantId)
                .eq(InviteCodeRecord::getStatus, "unused")
                .orderByDesc(InviteCodeRecord::getCreatedAt)
                .last("LIMIT 1");

        InviteCodeRecord oldRecord = inviteCodeRecordMapper.selectOne(queryWrapper);

        // 作废旧的邀请码
        if (oldRecord != null) {
            oldRecord.setStatus("invalidated");
            inviteCodeRecordMapper.updateById(oldRecord);
        }

        // 生成新的邀请码
        String inviteCode = generateUniqueCode();

        InviteCodeRecord record = new InviteCodeRecord();
        record.setTenantId(tenantId);
        record.setInviteCode(inviteCode);
        record.setStatus("unused");
        record.setExpireTime(LocalDateTime.now().plusDays(DEFAULT_EXPIRE_DAYS));
        inviteCodeRecordMapper.insert(record);

        InviteCodeResponse response = InviteCodeResponse.builder()
                .inviteCode(inviteCode)
                .expireTime(record.getExpireTime().format(DATE_TIME_FORMATTER))
                .status("unused")
                .build();

        return Result.success("邀请码已重新生成", response);
    }

    private String generateUniqueCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String prefix = "TENANT" + LocalDateTime.now().getYear();
        return prefix + uuid.substring(0, 8);
    }
}
