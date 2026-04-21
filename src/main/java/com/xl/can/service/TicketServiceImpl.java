package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.context.UserContext;
import com.xl.can.dto.*;
import com.xl.can.entity.*;
import com.xl.can.mapper.*;
import com.xl.can.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private TicketTimelineMapper timelineMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ====== 工单列表 ======
    @Override
    public Result<PageResult<TicketListVO>> list(TicketListDTO dto) {
        SysUser user = UserContext.getUser();
        String roleCode = user.getRoleCode();
        Long tenantId = user.getTenantId();
        Long userShopId = user.getShopId();

        Page<Ticket> pageParam = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Ticket::getDeletedAt, 0);
        wrapper.eq(Ticket::getTenantId, tenantId);

        // 店长只能看自己门店
        if ("shop_owner".equals(roleCode)) {
            wrapper.eq(Ticket::getShopId, userShopId);
        } else if (dto.getShopId() != null) {
            wrapper.eq(Ticket::getShopId, dto.getShopId());
        }

        if (dto.getTypeId() != null) {
            wrapper.eq(Ticket::getTypeId, dto.getTypeId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Ticket::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(Ticket::getTitle, dto.getKeyword())
                    .or().like(Ticket::getDescription, dto.getKeyword()));
        }
        if (StringUtils.hasText(dto.getStartDate())) {
            wrapper.ge(Ticket::getCreatedAt, dto.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(dto.getEndDate())) {
            wrapper.le(Ticket::getCreatedAt, dto.getEndDate() + " 23:59:59");
        }

        // 按截止时间升序排序（无截止时间视为最晚），紧急的排在前面
        // 使用 SQL: ORDER BY IF(deadline IS NULL, '2099-12-31', deadline) ASC, created_at DESC
        wrapper.last("ORDER BY IF(deadline IS NULL, '2099-12-31 23:59:59', deadline) ASC, created_at DESC");
        Page<Ticket> page = ticketMapper.selectPage(pageParam, wrapper);

        // 加载门店名称
        List<Long> shopIds = page.getRecords().stream().map(Ticket::getShopId).distinct().collect(Collectors.toList());
        Map<Long, String> shopNameMap = shopIds.isEmpty() ? Collections.emptyMap() :
                shopMapper.selectBatchIds(shopIds).stream()
                        .collect(Collectors.toMap(Shop::getId, Shop::getShopName, (a, b) -> a));

        // 加载类型名称
        List<Long> typeIds = page.getRecords().stream().map(Ticket::getTypeId).distinct().collect(Collectors.toList());
        Map<Long, String> typeNameMap = typeIds.isEmpty() ? Collections.emptyMap() :
                ticketTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(TicketType::getId, TicketType::getName, (a, b) -> a));

        // 加载店长名称
        List<Long> assigneeIds = page.getRecords().stream()
                .map(Ticket::getAssigneeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> assigneeNameMap = assigneeIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(assigneeIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));

        List<TicketListVO> voList = page.getRecords().stream().map(t -> {
            TicketListVO vo = new TicketListVO();
            vo.setId(t.getId());
            vo.setTitle(t.getTitle());
            vo.setTypeId(t.getTypeId());
            vo.setTypeName(typeNameMap.getOrDefault(t.getTypeId(), ""));
            vo.setShopId(t.getShopId());
            vo.setShopName(shopNameMap.getOrDefault(t.getShopId(), ""));
            vo.setStatus(t.getStatus());
            vo.setStatusName(getStatusName(t.getStatus(), "shop".equals(dto.getViewAs()) ? "shop" : "tenant"));
            vo.setPriority(t.getPriority());
            vo.setDeadline(t.getDeadline() != null ? t.getDeadline().format(DT_FORMATTER) : null);
            vo.setCreatorId(t.getCreatorId());
            vo.setCreatorName("system".equals(getCreatorName(t.getCreatorId())) ? "系统" : getCreatorName(t.getCreatorId()));
            vo.setAssigneeId(t.getAssigneeId());
            vo.setAssigneeName(assigneeNameMap.getOrDefault(t.getAssigneeId(), ""));
            vo.setReviewId(t.getReviewId());
            // 差评星级
            if (t.getReviewId() != null) {
                Review r = reviewMapper.selectById(t.getReviewId());
                if (r != null) vo.setReviewStar(r.getStarRating());
            }
            vo.setCreatedAt(t.getCreatedAt());
            vo.setUpdatedAt(t.getUpdatedAt());
            // 动态计算优先级
            vo.calculateDynamicPriority();
            return vo;
        }).collect(Collectors.toList());

        return Result.success(PageResult.of(voList, page.getTotal(), dto.getPage(), dto.getPageSize()));
    }

    // ====== 工单详情 ======
    @Override
    public Result<TicketDetailVO> getDetail(Long id) {
        SysUser user = UserContext.getUser();
        String roleCode = user.getRoleCode();
        Long tenantId = user.getTenantId();
        Long userShopId = user.getShopId();

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (!ticket.getTenantId().equals(tenantId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该工单");
        }
        if ("shop_owner".equals(roleCode) && !ticket.getShopId().equals(userShopId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该工单");
        }

        TicketDetailVO vo = buildDetailVO(ticket, tenantId);
        return Result.success(vo);
    }

    // ====== 创建工单 ======
    @Override
    @Transactional
    public Result<TicketListVO> create(TicketCreateDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"tenant_admin".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可创建工单");
        }

        // 校验必填
        if (!StringUtils.hasText(dto.getTitle())) {
            return Result.error(ResultCode.BAD_REQUEST, "工单标题不能为空");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            return Result.error(ResultCode.BAD_REQUEST, "工单描述不能为空");
        }
        if (dto.getTypeId() == null) {
            return Result.error(ResultCode.BAD_REQUEST, "请选择工单类型");
        }
        if (dto.getShopId() == null) {
            return Result.error(ResultCode.BAD_REQUEST, "请选择关联门店");
        }

        // 校验类型
        TicketType type = ticketTypeMapper.selectById(dto.getTypeId());
        if (type == null || type.getStatus() != 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单类型不存在");
        }

        // 差评预警类型必须关联评价
        if (Boolean.TRUE.equals(type.getSupportReview())) {
            if (dto.getReviewId() == null) {
                return Result.error(ResultCode.BAD_REQUEST, "差评预警类型必须关联一个评价");
            }
            Review review = reviewMapper.selectById(dto.getReviewId());
            if (review == null) {
                return Result.error(ResultCode.NOT_FOUND, "关联的评价不存在");
            }
            if (!review.getShopId().equals(dto.getShopId())) {
                return Result.error(ResultCode.BAD_REQUEST, "关联的评价必须属于所选门店");
            }
            // 检查是否已关联
            LambdaQueryWrapper<Ticket> reviewCheck = new LambdaQueryWrapper<>();
            reviewCheck.eq(Ticket::getReviewId, dto.getReviewId())
                    .eq(Ticket::getDeletedAt, 0)
                    .ne(Ticket::getStatus, 4)
                    .ne(Ticket::getStatus, 5);
            if (ticketMapper.selectCount(reviewCheck) > 0) {
                return Result.error(ResultCode.CONFLICT, "该评价已关联工单");
            }
        }

        // 校验门店
        Shop shop = shopMapper.selectById(dto.getShopId());
        if (shop == null || shop.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "门店不存在");
        }

        // 自动派单：根据门店查找该门店店长
        SysUser shopOwner = null;
        LambdaQueryWrapper<SysUser> ownerWrapper = new LambdaQueryWrapper<>();
        ownerWrapper.eq(SysUser::getShopId, dto.getShopId())
                .eq(SysUser::getRoleCode, "shop_owner")
                .eq(SysUser::getStatus, 1);
        List<SysUser> owners = sysUserMapper.selectList(ownerWrapper);
        if (!owners.isEmpty()) {
            shopOwner = owners.get(0);
        }
        // 门店必须有关联的店长才能创建工单
        if (shopOwner == null) {
            return Result.error(ResultCode.BAD_REQUEST, "请先给「" + shop.getShopName() + "」门店分配店长");
        }

        // 计算优先级
        String priority = "low";
        if (StringUtils.hasText(dto.getDeadline())) {
            try {
                LocalDateTime deadline = LocalDateTime.parse(dto.getDeadline(), DT_FORMATTER);
                long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
                if (hours <= 24) priority = "high";
                else if (hours <= 72) priority = "medium";
            } catch (Exception ignored) {}
        }

        // 生成工单编号
        String ticketNo = generateTicketNo(user.getTenantId());

        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNo);
        ticket.setTypeId(dto.getTypeId());
        ticket.setShopId(dto.getShopId());
        ticket.setTenantId(user.getTenantId());
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(priority);
        if (StringUtils.hasText(dto.getDeadline())) {
            try {
                ticket.setDeadline(LocalDateTime.parse(dto.getDeadline(), DT_FORMATTER));
            } catch (Exception ignored) {}
        }
        ticket.setReviewId(dto.getReviewId());
        ticket.setCreatorId(user.getId());
        ticket.setDeletedAt(0);
        ticket.setImages(toJson(dto.getImages()));
        // 初始状态设为待处理，后续自动派单时更新为处理中
        // 自动派单：如果该门店有店长，工单状态直接设为处理中
        if (shopOwner != null) {
            ticket.setAssigneeId(shopOwner.getId());
            ticket.setStatus(2);
        } else {
            ticket.setStatus(1);
        }
        ticketMapper.insert(ticket);

        // 记录创建时间线
        addTimeline(ticket.getId(), user.getTenantId(), "created", user.getId(), user.getRoleCode(), "创建工单");

        // 如果有店长，记录派单时间线
        if (shopOwner != null) {
            addTimeline(ticket.getId(), user.getTenantId(), "assigned", shopOwner.getId(), "shop_owner",
                    "自动派单给" + shopOwner.getRealName() + "处理");
        }

        TicketListVO vo = new TicketListVO();
        vo.setId(ticket.getId());
        vo.setTitle(ticket.getTitle());
        vo.setStatus(ticket.getStatus());
        vo.setPriority(ticket.getPriority());
        vo.setDeadline(ticket.getDeadline() != null ? ticket.getDeadline().format(DT_FORMATTER) : null);
        vo.setCreatedAt(ticket.getCreatedAt());
        return Result.success("创建成功", vo);
    }

    // ====== 派单 ======
    @Override
    @Transactional
    public Result<Void> assign(Long id, TicketAssignDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"tenant_admin".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可派单");
        }
        if (dto.getAssigneeId() == null) {
            return Result.error(ResultCode.BAD_REQUEST, "请选择处理人");
        }

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (ticket.getStatus() != 1) {
            return Result.error(ResultCode.BAD_REQUEST, "当前状态不允许派单");
        }

        // 校验处理人是该门店店长
        SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
        if (assignee == null || !"shop_owner".equals(assignee.getRoleCode())) {
            return Result.error(ResultCode.BAD_REQUEST, "处理人必须是店长");
        }
        if (!assignee.getShopId().equals(ticket.getShopId())) {
            return Result.error(ResultCode.BAD_REQUEST, "处理人必须是该门店的店长");
        }

        ticket.setAssigneeId(dto.getAssigneeId());
        ticket.setStatus(2);
        ticketMapper.updateById(ticket);

        String content = "派单给" + assignee.getRealName() + "处理" +
                (StringUtils.hasText(dto.getNote()) ? "：" + dto.getNote() : "");
        addTimeline(ticket.getId(), user.getTenantId(), "assigned", user.getId(), user.getRoleCode(), content);

        return Result.success("派单成功", null);
    }

    // ====== 店长提交整改 ======
    @Override
    @Transactional
    public Result<Void> submit(Long id, TicketActionDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"shop_owner".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅店长可提交整改");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            return Result.error(ResultCode.BAD_REQUEST, "整改说明不能为空");
        }

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (!ticket.getShopId().equals(user.getShopId())) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作该工单");
        }
        if (ticket.getStatus() != 1 && ticket.getStatus() != 2) {
            return Result.error(ResultCode.BAD_REQUEST, "当前状态不允许提交整改");
        }

        ticket.setStatus(3);
        ticket.setSolution(dto.getContent());
        ticketMapper.updateById(ticket);

        addTimeline(ticket.getId(), user.getTenantId(), "submit", user.getId(), user.getRoleCode(), dto.getContent());

        return Result.success("提交成功", null);
    }

    // ====== 核销 ======
    @Override
    @Transactional
    public Result<Void> verify(Long id, TicketActionDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"tenant_admin".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可核销");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            return Result.error(ResultCode.BAD_REQUEST, "核销说明不能为空");
        }

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (ticket.getStatus() != 3) {
            return Result.error(ResultCode.BAD_REQUEST, "当前状态不允许核销");
        }

        ticket.setStatus(4);
        ticketMapper.updateById(ticket);

        addTimeline(ticket.getId(), user.getTenantId(), "verified", user.getId(), user.getRoleCode(), dto.getContent());

        return Result.success("核销成功", null);
    }

    // ====== 归档 ======
    @Override
    @Transactional
    public Result<Void> archive(Long id, TicketActionDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"tenant_admin".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可归档");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            return Result.error(ResultCode.BAD_REQUEST, "归档原因不能为空");
        }

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (ticket.getStatus() == 4 || ticket.getStatus() == 5) {
            return Result.error(ResultCode.BAD_REQUEST, "该工单已结束");
        }

        ticket.setStatus(4);
        ticketMapper.updateById(ticket);

        addTimeline(ticket.getId(), user.getTenantId(), "archived", user.getId(), user.getRoleCode(), dto.getContent());

        return Result.success("归档成功", null);
    }

    // ====== 打回 ======
    @Override
    @Transactional
    public Result<Void> reject(Long id, TicketRejectDTO dto) {
        SysUser user = UserContext.getUser();
        if (!"tenant_admin".equals(user.getRoleCode())) {
            return Result.error(ResultCode.FORBIDDEN, "仅租户管理员可打回");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            return Result.error(ResultCode.BAD_REQUEST, "打回原因不能为空");
        }

        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || ticket.getDeletedAt() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "工单不存在");
        }
        if (ticket.getStatus() != 2 && ticket.getStatus() != 3) {
            return Result.error(ResultCode.BAD_REQUEST, "当前状态不允许打回");
        }

        // 直接打回给当前处理人
        SysUser assignee = sysUserMapper.selectById(ticket.getAssigneeId());
        String assigneeName = assignee != null ? assignee.getRealName() : "该店长";

        // 状态改为处理中，让店长可以继续处理
        ticket.setStatus(2);

        // 如果提供了新的截止时间，则更新
        if (StringUtils.hasText(dto.getDeadline())) {
            try {
                ticket.setDeadline(LocalDateTime.parse(dto.getDeadline(), DT_FORMATTER));
            } catch (Exception e) {
                return Result.error(ResultCode.BAD_REQUEST, "截止时间格式不正确");
            }
        }

        ticketMapper.updateById(ticket);

        String content = "打回给" + assigneeName + "：" + dto.getContent();
        if (StringUtils.hasText(dto.getDeadline())) {
            content += "，截止时间已更新为" + dto.getDeadline();
        }
        addTimeline(ticket.getId(), user.getTenantId(), "rejected", user.getId(), user.getRoleCode(), content);

        return Result.success("打回成功", null);
    }

    // ====== 工单统计 ======
    @Override
    public Result<TicketStatisticsVO> statistics(TicketStatisticsDTO dto) {
        SysUser user = UserContext.getUser();
        String roleCode = user.getRoleCode();
        Long tenantId = user.getTenantId();
        Long userShopId = user.getShopId();

        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getDeletedAt, 0);
        wrapper.eq(Ticket::getTenantId, tenantId);

        if ("shop_owner".equals(roleCode)) {
            wrapper.eq(Ticket::getShopId, userShopId);
        } else if (dto.getShopId() != null) {
            wrapper.eq(Ticket::getShopId, dto.getShopId());
        }

        List<Ticket> tickets = ticketMapper.selectList(wrapper);

        TicketStatisticsVO vo = new TicketStatisticsVO();
        vo.setTotalCount(tickets.size());
        vo.setPendingCount((int) tickets.stream().filter(t -> t.getStatus() == 1).count());
        vo.setProcessingCount((int) tickets.stream().filter(t -> t.getStatus() == 2).count());
        vo.setPendingVerifyCount((int) tickets.stream().filter(t -> t.getStatus() == 3).count());
        vo.setArchivedCount((int) tickets.stream().filter(t -> t.getStatus() == 4 || t.getStatus() == 5).count());
        vo.setOverdueCount((int) tickets.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(LocalDateTime.now())
                        && t.getStatus() != 4 && t.getStatus() != 5)
                .count());

        TicketStatisticsVO.PriorityStats priorityStats = new TicketStatisticsVO.PriorityStats();
        priorityStats.setHigh((int) tickets.stream().filter(t -> "high".equals(t.getPriority())).count());
        priorityStats.setMedium((int) tickets.stream().filter(t -> "medium".equals(t.getPriority())).count());
        priorityStats.setLow((int) tickets.stream().filter(t -> "low".equals(t.getPriority())).count());
        vo.setPriorityStats(priorityStats);

        // 类型分布：按类型ID统计
        Map<Long, Long> typeCountMap = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getTypeId, Collectors.counting()));

        TicketStatisticsVO.TypeStats typeStats = new TicketStatisticsVO.TypeStats();
        typeStats.setNegativeReview(0);
        typeStats.setComplaint(0);
        typeStats.setMetric(0);
        typeStats.setCustom(0);

        for (Map.Entry<Long, Long> entry : typeCountMap.entrySet()) {
            TicketType t = ticketTypeMapper.selectById(entry.getKey());
            if (t != null) {
                String name = t.getName();
                if ("差评预警".equals(name)) typeStats.setNegativeReview(entry.getValue().intValue());
                else if ("投诉预警".equals(name)) typeStats.setComplaint(entry.getValue().intValue());
                else if ("指标预警".equals(name)) typeStats.setMetric(entry.getValue().intValue());
                else typeStats.setCustom(typeStats.getCustom() + entry.getValue().intValue());
            } else {
                typeStats.setCustom(typeStats.getCustom() + entry.getValue().intValue());
            }
        }
        vo.setTypeStats(typeStats);

        return Result.success(vo);
    }

    // ====== 评价列表（用于关联差评预警）======
    @Override
    public Result<PageResult<ReviewForTicketVO>> getReviewsForTicket(Long shopId, Integer starRating, Integer page, Integer pageSize) {
        SysUser user = UserContext.getUser();
        Long tenantId = user.getTenantId();
        Long userShopId = user.getShopId();
        String roleCode = user.getRoleCode();

        // 获取有效门店
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, tenantId).eq(Shop::getDeletedAt, 0);
        List<Shop> shops = shopMapper.selectList(shopWrapper);
        List<Long> validShopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());
        if (validShopIds.isEmpty()) return Result.success(PageResult.empty());

        Page<Review> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Review::getShopId, validShopIds);

        if ("shop_owner".equals(roleCode)) {
            wrapper.eq(Review::getShopId, userShopId);
        } else if (shopId != null) {
            wrapper.eq(Review::getShopId, shopId);
        }

        if (starRating != null) {
            wrapper.le(Review::getStarRating, starRating);
        }

        // 只查未关联有效工单的评价
        wrapper.notInSql(true, Review::getId,
                "SELECT review_id FROM ticket WHERE deleted_at = 0 AND status NOT IN (4,5) AND review_id IS NOT NULL");

        wrapper.orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(pageParam, wrapper);

        // 获取已关联的reviewId
        LambdaQueryWrapper<Ticket> linkedWrapper = new LambdaQueryWrapper<>();
        linkedWrapper.eq(Ticket::getDeletedAt, 0)
                .ne(Ticket::getStatus, 4)
                .ne(Ticket::getStatus, 5)
                .isNotNull(Ticket::getReviewId);
        List<Long> linkedReviewIds = ticketMapper.selectList(linkedWrapper).stream()
                .map(Ticket::getReviewId)
                .collect(Collectors.toList());

        // 门店名称映射
        Map<Long, String> shopNameMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getShopName, (a, b) -> a));

        List<ReviewForTicketVO> voList = result.getRecords().stream().map(r -> {
            ReviewForTicketVO vo = new ReviewForTicketVO();
            vo.setId(r.getId());
            vo.setPlatform(r.getPlatform());
            vo.setPlatformOrderId(r.getPlatformOrderId());
            vo.setShopId(r.getShopId());
            vo.setShopName(shopNameMap.getOrDefault(r.getShopId(), ""));
            vo.setStarRating(r.getStarRating());
            vo.setContent(r.getContent());
            vo.setCreatedAt(r.getCreatedAt());
            vo.setAlreadyLinked(linkedReviewIds.contains(r.getId()));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(PageResult.of(voList, result.getTotal(), page, pageSize));
    }

    // ====== 私有方法 ======

    private TicketDetailVO buildDetailVO(Ticket ticket, Long tenantId) {
        TicketDetailVO vo = new TicketDetailVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setDescription(ticket.getDescription());
        vo.setTypeId(ticket.getTypeId());
        TicketType type = ticketTypeMapper.selectById(ticket.getTypeId());
        if (type != null) vo.setTypeName(type.getName());
        vo.setShopId(ticket.getShopId());
        Shop shop = shopMapper.selectById(ticket.getShopId());
        if (shop != null) vo.setShopName(shop.getShopName());
        vo.setStatus(ticket.getStatus());
        vo.setStatusName(getStatusName(ticket.getStatus(), "tenant"));
        vo.setPriority(ticket.getPriority());
        vo.setDeadline(ticket.getDeadline() != null ? ticket.getDeadline().format(DT_FORMATTER) : null);
        vo.setCreatorId(ticket.getCreatorId());
        SysUser creator = sysUserMapper.selectById(ticket.getCreatorId());
        String creatorName = "系统";
        if (creator != null) {
            creatorName = StringUtils.hasText(creator.getRealName()) ? creator.getRealName() : creator.getUsername();
        }
        vo.setCreatorName(creatorName);
        vo.setAssigneeId(ticket.getAssigneeId());
        SysUser assignee = sysUserMapper.selectById(ticket.getAssigneeId());
        vo.setAssigneeName(assignee != null ? assignee.getRealName() : "");
        vo.setReviewId(ticket.getReviewId());

        // 关联评价详情
        if (ticket.getReviewId() != null) {
            Review review = reviewMapper.selectById(ticket.getReviewId());
            if (review != null) {
                ReviewVO reviewVO = new ReviewVO();
                reviewVO.setId(review.getId());
                reviewVO.setPlatform(review.getPlatform());
                reviewVO.setPlatformOrderId(review.getPlatformOrderId());
                reviewVO.setStarRating(review.getStarRating());
                reviewVO.setContent(review.getContent());
                reviewVO.setCreatedAt(review.getCreatedAt());
                vo.setReview(reviewVO);
            }
        }

        // 图片列表
        if (StringUtils.hasText(ticket.getImages())) {
            try {
                vo.setImages(objectMapper.readValue(ticket.getImages(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                vo.setImages(new ArrayList<>());
            }
        } else {
            vo.setImages(new ArrayList<>());
        }

        // 时间线
        LambdaQueryWrapper<TicketTimeline> timelineWrapper = new LambdaQueryWrapper<>();
        timelineWrapper.eq(TicketTimeline::getTicketId, ticket.getId())
                .orderByAsc(TicketTimeline::getCreatedAt);
        List<TicketTimeline> timelines = timelineMapper.selectList(timelineWrapper);
        List<TicketTimelineVO> logs = timelines.stream().map(t -> {
            TicketTimelineVO log = new TicketTimelineVO();
            log.setId(t.getId());
            log.setAction(t.getAction());
            log.setOperatorId(t.getOperatorId());
            SysUser op = sysUserMapper.selectById(t.getOperatorId());
            log.setOperatorName(op != null ? op.getRealName() : "系统");
            log.setOperatorRole(t.getOperatorId() == null ? "system" : (sysUserMapper.selectById(t.getOperatorId()) != null ?
                    sysUserMapper.selectById(t.getOperatorId()).getRoleCode() : "system"));
            log.setContent(t.getRemark());
            log.setCreatedAt(t.getCreatedAt());
            return log;
        }).collect(Collectors.toList());
        vo.setLogs(logs);

        vo.setCreatedAt(ticket.getCreatedAt());
        vo.setUpdatedAt(ticket.getUpdatedAt());
        // 动态计算优先级
        vo.calculateDynamicPriority();
        return vo;
    }

    private void addTimeline(Long ticketId, Long tenantId, String action, Long operatorId, String roleCode, String remark) {
        TicketTimeline timeline = new TicketTimeline();
        timeline.setTicketId(ticketId);
        timeline.setTenantId(tenantId);
        timeline.setAction(action);
        timeline.setOperatorId(operatorId);
        timeline.setRemark(remark);
        timelineMapper.insert(timeline);
    }

    private String getStatusName(Integer status, String viewAs) {
        if ("shop".equals(viewAs)) {
            return switch (status) {
                case 1 -> "待处理";
                case 2 -> "处理中";
                case 3 -> "提交整改";
                case 4 -> "已归档";
                case 5 -> "已忽略";
                default -> "未知";
            };
        } else {
            return switch (status) {
                case 1 -> "待处理(待派单)";
                case 2 -> "处理中";
                case 3 -> "待核销";
                case 4 -> "已归档";
                case 5 -> "已忽略";
                default -> "未知";
            };
        }
    }

    private String getCreatorName(Long creatorId) {
        if (creatorId == null) return "系统";
        SysUser u = sysUserMapper.selectById(creatorId);
        return u != null ? u.getRealName() : "系统";
    }

    private String generateTicketNo(Long tenantId) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getTenantId, tenantId)
                .likeRight(Ticket::getTicketNo, "TK")
                .orderByDesc(Ticket::getTicketNo)
                .last("LIMIT 1");
        Ticket last = ticketMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null) {
            String no = last.getTicketNo();
            try {
                seq = Integer.parseInt(no.substring(no.length() - 6)) + 1;
            } catch (Exception ignored) {}
        }
        return "TK" + String.format("%06d", seq);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
