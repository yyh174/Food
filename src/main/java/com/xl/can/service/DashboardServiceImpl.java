package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.common.Result;
import com.xl.can.context.UserContext;
import com.xl.can.entity.Review;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysUser;
import com.xl.can.entity.Ticket;
import com.xl.can.mapper.ReviewMapper;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.mapper.SysUserMapper;
import com.xl.can.mapper.TicketMapper;
import com.xl.can.vo.DashboardVO;
import com.xl.can.vo.ReviewStatisticsVO;
import com.xl.can.vo.TicketStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public Result<DashboardVO> getStatistics() {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long tenantId = currentUser.getTenantId();
        Long userShopId = currentUser.getShopId();

        DashboardVO vo = new DashboardVO();
        vo.setRoleCode(roleCode);

        DashboardVO.StatCards statCards = new DashboardVO.StatCards();

        if ("tenant_admin".equals(roleCode)) {
            buildTenantAdminStats(tenantId, statCards);
        } else if ("shop_owner".equals(roleCode)) {
            buildShopOwnerStats(tenantId, userShopId, statCards);
        }

        vo.setStatCards(statCards);

        ReviewStatisticsVO reviewStats = getReviewStatistics(roleCode, tenantId, userShopId);
        vo.setReviewStatistics(reviewStats);

        TicketStatisticsVO ticketStats = getTicketStatistics(roleCode, tenantId, userShopId);
        vo.setTicketStatistics(ticketStats);

        return Result.success(vo);
    }

    private void buildTenantAdminStats(Long tenantId, DashboardVO.StatCards statCards) {
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, tenantId);
        shopWrapper.eq(Shop::getStatus, 1);
        long shopCount = shopMapper.selectCount(shopWrapper);
        statCards.setShopCount((int) shopCount);

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, tenantId);
        userWrapper.eq(SysUser::getStatus, 1);
        long userCount = sysUserMapper.selectCount(userWrapper);
        statCards.setUserCount((int) userCount);

        List<Shop> validShops = shopMapper.selectList(shopWrapper);
        List<Long> validShopIds = validShops.stream()
                .map(Shop::getId)
                .toList();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        if (!validShopIds.isEmpty()) {
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.in(Review::getShopId, validShopIds);
            reviewWrapper.ge(Review::getCreatedAt, todayStart);
            reviewWrapper.le(Review::getCreatedAt, todayEnd);
            long todayReviewCount = reviewMapper.selectCount(reviewWrapper);
            statCards.setTodayReviewCount((int) todayReviewCount);

            LambdaQueryWrapper<Ticket> ticketWrapper = new LambdaQueryWrapper<>();
            ticketWrapper.in(Ticket::getShopId, validShopIds);
            ticketWrapper.ge(Ticket::getCreatedAt, todayStart);
            ticketWrapper.le(Ticket::getCreatedAt, todayEnd);
            long todayTicketCount = ticketMapper.selectCount(ticketWrapper);
            statCards.setTodayTicketCount((int) todayTicketCount);

            long pendingTicketCount = countPendingTickets(validShopIds);
            statCards.setPendingTicketCount((int) pendingTicketCount);
        } else {
            statCards.setTodayReviewCount(0);
            statCards.setTodayTicketCount(0);
            statCards.setPendingTicketCount(0);
        }

        statCards.setPendingReviewCount(0);
        statCards.setRepliedReviewCount(0);
        statCards.setTotalReviewCount(0);
    }

    private void buildShopOwnerStats(Long tenantId, Long userShopId, DashboardVO.StatCards statCards) {
        statCards.setShopCount(1);

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, tenantId);
        userWrapper.eq(SysUser::getShopId, userShopId);
        userWrapper.eq(SysUser::getStatus, 1);
        long shopUserCount = sysUserMapper.selectCount(userWrapper);
        statCards.setUserCount((int) shopUserCount);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getShopId, userShopId);

        LambdaQueryWrapper<Review> todayReviewWrapper = new LambdaQueryWrapper<>();
        todayReviewWrapper.eq(Review::getShopId, userShopId);
        todayReviewWrapper.ge(Review::getCreatedAt, todayStart);
        todayReviewWrapper.le(Review::getCreatedAt, todayEnd);
        long todayReviewCount = reviewMapper.selectCount(todayReviewWrapper);
        statCards.setTodayReviewCount((int) todayReviewCount);

        long totalReviewCount = reviewMapper.selectCount(reviewWrapper);
        statCards.setTotalReviewCount((int) totalReviewCount);

        LambdaQueryWrapper<Review> repliedWrapper = new LambdaQueryWrapper<>();
        repliedWrapper.eq(Review::getShopId, userShopId);
        repliedWrapper.isNotNull(Review::getReplyContent);
        long repliedReviewCount = reviewMapper.selectCount(repliedWrapper);
        statCards.setRepliedReviewCount((int) repliedReviewCount);

        LambdaQueryWrapper<Review> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Review::getShopId, userShopId);
        pendingWrapper.isNull(Review::getReplyContent);
        long pendingReviewCount = reviewMapper.selectCount(pendingWrapper);
        statCards.setPendingReviewCount((int) pendingReviewCount);

        LambdaQueryWrapper<Ticket> ticketWrapper = new LambdaQueryWrapper<>();
        ticketWrapper.eq(Ticket::getShopId, userShopId);

        LambdaQueryWrapper<Ticket> todayTicketWrapper = new LambdaQueryWrapper<>();
        todayTicketWrapper.eq(Ticket::getShopId, userShopId);
        todayTicketWrapper.ge(Ticket::getCreatedAt, todayStart);
        todayTicketWrapper.le(Ticket::getCreatedAt, todayEnd);
        long todayTicketCount = ticketMapper.selectCount(todayTicketWrapper);
        statCards.setTodayTicketCount((int) todayTicketCount);

        LambdaQueryWrapper<Ticket> pendingTicketWrapper = new LambdaQueryWrapper<>();
        pendingTicketWrapper.eq(Ticket::getShopId, userShopId);
        pendingTicketWrapper.eq(Ticket::getStatus, 0);
        long pendingTicketCount = ticketMapper.selectCount(pendingTicketWrapper);
        statCards.setPendingTicketCount((int) pendingTicketCount);
    }

    private long countPendingTickets(List<Long> shopIds) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Ticket::getShopId, shopIds);
        wrapper.eq(Ticket::getStatus, 0);
        return ticketMapper.selectCount(wrapper);
    }

    private ReviewStatisticsVO getReviewStatistics(String roleCode, Long tenantId, Long userShopId) {
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();

        if ("shop_owner".equals(roleCode)) {
            queryWrapper.eq(Review::getShopId, userShopId);
        } else {
            LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
            shopWrapper.eq(Shop::getTenantId, tenantId);
            List<Shop> validShops = shopMapper.selectList(shopWrapper);
            List<Long> validShopIds = validShops.stream().map(Shop::getId).toList();
            if (!validShopIds.isEmpty()) {
                queryWrapper.in(Review::getShopId, validShopIds);
            }
        }

        List<Review> allReviews = reviewMapper.selectList(queryWrapper);

        ReviewStatisticsVO vo = new ReviewStatisticsVO();

        int totalCount = allReviews.size();
        int replyCount = (int) allReviews.stream()
                .filter(r -> r.getReplyContent() != null && !r.getReplyContent().isEmpty())
                .count();
        int unReplyCount = totalCount - replyCount;
        double replyRate = totalCount > 0 ? (replyCount * 100.0 / totalCount) : 0.0;

        vo.setTotalCount(totalCount);
        vo.setReplyCount(replyCount);
        vo.setUnReplyCount(unReplyCount);
        vo.setReplyRate(Math.round(replyRate * 10) / 10.0);

        double avgRating = allReviews.stream()
                .mapToInt(r -> r.getStarRating() != null ? r.getStarRating() : 0)
                .average()
                .orElse(0.0);
        vo.setAvgStarRating(Math.round(avgRating * 10) / 10.0);

        ReviewStatisticsVO.SentimentStats sentimentStats = new ReviewStatisticsVO.SentimentStats();
        int positive = (int) allReviews.stream()
                .filter(r -> r.getStarRating() != null && r.getStarRating() >= 4)
                .count();
        int neutral = (int) allReviews.stream()
                .filter(r -> r.getStarRating() != null && r.getStarRating() == 3)
                .count();
        int negative = (int) allReviews.stream()
                .filter(r -> r.getStarRating() != null && r.getStarRating() <= 2)
                .count();
        sentimentStats.setPositive(positive);
        sentimentStats.setNeutral(neutral);
        sentimentStats.setNegative(negative);
        vo.setSentimentStats(sentimentStats);

        ReviewStatisticsVO.PlatformStats platformStats = new ReviewStatisticsVO.PlatformStats();
        int meituanCount = (int) allReviews.stream()
                .filter(r -> r.getPlatform() != null && r.getPlatform() == 1)
                .count();
        int eleCount = (int) allReviews.stream()
                .filter(r -> r.getPlatform() != null && r.getPlatform() == 2)
                .count();
        platformStats.setMeituan(meituanCount);
        platformStats.setEle(eleCount);
        vo.setPlatformStats(platformStats);

        return vo;
    }

    private TicketStatisticsVO getTicketStatistics(String roleCode, Long tenantId, Long userShopId) {
        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();

        if ("shop_owner".equals(roleCode)) {
            queryWrapper.eq(Ticket::getShopId, userShopId);
        } else {
            LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
            shopWrapper.eq(Shop::getTenantId, tenantId);
            List<Shop> validShops = shopMapper.selectList(shopWrapper);
            List<Long> validShopIds = validShops.stream().map(Shop::getId).toList();
            if (!validShopIds.isEmpty()) {
                queryWrapper.in(Ticket::getShopId, validShopIds);
            }
        }

        List<Ticket> allTickets = ticketMapper.selectList(queryWrapper);

        TicketStatisticsVO vo = new TicketStatisticsVO();

        int totalCount = allTickets.size();
        int pendingCount = (int) allTickets.stream().filter(t -> t.getStatus() != null && t.getStatus() == 0).count();
        int processingCount = (int) allTickets.stream().filter(t -> t.getStatus() != null && t.getStatus() == 1).count();
        int pendingVerifyCount = (int) allTickets.stream().filter(t -> t.getStatus() != null && t.getStatus() == 2).count();
        int archivedCount = (int) allTickets.stream().filter(t -> t.getStatus() != null && t.getStatus() == 3).count();

        vo.setTotalCount(totalCount);
        vo.setPendingCount(pendingCount);
        vo.setProcessingCount(processingCount);
        vo.setPendingVerifyCount(pendingVerifyCount);
        vo.setArchivedCount(archivedCount);
        vo.setOverdueCount(0);

        TicketStatisticsVO.PriorityStats priorityStats = new TicketStatisticsVO.PriorityStats();
        priorityStats.setHigh((int) allTickets.stream().filter(t -> "high".equals(t.getPriority())).count());
        priorityStats.setMedium((int) allTickets.stream().filter(t -> "medium".equals(t.getPriority())).count());
        priorityStats.setLow((int) allTickets.stream().filter(t -> "low".equals(t.getPriority())).count());
        vo.setPriorityStats(priorityStats);

        TicketStatisticsVO.TypeStats typeStats = new TicketStatisticsVO.TypeStats();
        typeStats.setNegativeReview((int) allTickets.stream().filter(t -> t.getTypeId() != null && t.getTypeId() == 1).count());
        typeStats.setComplaint((int) allTickets.stream().filter(t -> t.getTypeId() != null && t.getTypeId() == 2).count());
        typeStats.setMetric((int) allTickets.stream().filter(t -> t.getTypeId() != null && t.getTypeId() == 3).count());
        typeStats.setCustom((int) allTickets.stream().filter(t -> t.getTypeId() != null && t.getTypeId() == 4).count());
        vo.setTypeStats(typeStats);

        return vo;
    }
}
