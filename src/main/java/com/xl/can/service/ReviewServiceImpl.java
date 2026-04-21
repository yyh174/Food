package com.xl.can.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.common.ResultCode;
import com.xl.can.context.UserContext;
import com.xl.can.dto.ReviewListDTO;
import com.xl.can.dto.ReviewReplyRequest;
import com.xl.can.dto.ReviewReplyTemplateRequest;
import com.xl.can.dto.ReviewStatisticsDTO;
import com.xl.can.entity.ReplyTemplate;
import com.xl.can.entity.Review;
import com.xl.can.entity.Shop;
import com.xl.can.entity.SysUser;
import com.xl.can.mapper.ReplyTemplateMapper;
import com.xl.can.mapper.ReviewMapper;
import com.xl.can.mapper.ShopMapper;
import com.xl.can.vo.ReviewStatisticsVO;
import com.xl.can.vo.ReviewVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ReplyTemplateMapper replyTemplateMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result<PageResult<ReviewVO>> pageList(ReviewListDTO dto) {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long tenantId = currentUser.getTenantId();
        Long userShopId = currentUser.getShopId();

        Page<Review> pageParam = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();

        // 获取当前租户下未删除的门店ID列表（用于过滤已删除门店的评价）
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, tenantId);
        List<Shop> validShops = shopMapper.selectList(shopWrapper);
        List<Long> validShopIds = validShops.stream()
                .map(Shop::getId)
                .collect(Collectors.toList());

        if (validShopIds.isEmpty()) {
            return Result.success(PageResult.empty());
        }
        queryWrapper.in(Review::getShopId, validShopIds);

        // 权限控制：店长只能查看自己门店的评价
        if ("shop_owner".equals(roleCode)) {
            queryWrapper.eq(Review::getShopId, userShopId);
        } else if ("tenant_admin".equals(roleCode)) {
            queryWrapper.eq(Review::getTenantId, tenantId);
            // 租户管理员可按门店筛选
            if (dto.getShopId() != null) {
                queryWrapper.eq(Review::getShopId, dto.getShopId());
            }
        }

        // 平台筛选
        if (dto.getPlatform() != null) {
            queryWrapper.eq(Review::getPlatform, dto.getPlatform());
        }

        // 回复状态筛选
        if (dto.getReplyStatus() != null) {
            if (dto.getReplyStatus() == 0) {
                queryWrapper.isNull(Review::getReplyContent);
            } else {
                queryWrapper.isNotNull(Review::getReplyContent);
            }
        }

        // 关键词搜索（评价内容）
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            queryWrapper.like(Review::getContent, dto.getKeyword());
        }

        // 日期范围筛选
        if (dto.getStartDate() != null) {
            queryWrapper.ge(Review::getCreatedAt, dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            queryWrapper.le(Review::getCreatedAt, dto.getEndDate().atTime(LocalTime.MAX));
        }

        queryWrapper.orderByDesc(Review::getCreatedAt);

        Page<Review> result = reviewMapper.selectPage(pageParam, queryWrapper);

        // 获取门店名称映射
        List<Long> shopIds = result.getRecords().stream()
                .map(Review::getShopId)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, String> shopNameMap = shopIds.isEmpty() ? new java.util.HashMap<>() :
                shopMapper.selectBatchIds(shopIds).stream()
                        .collect(Collectors.toMap(Shop::getId, Shop::getShopName, (a, b) -> a));

        List<ReviewVO> voList = result.getRecords().stream().map(review -> {
            ReviewVO vo = convertToVO(review);
            vo.setShopName(shopNameMap.get(review.getShopId()));
            return vo;
        }).collect(Collectors.toList());

        PageResult<ReviewVO> pageResult = PageResult.of(voList, result.getTotal(), dto.getPage(), dto.getPageSize());
        return Result.success(pageResult);
    }

    @Override
    public Result<ReviewVO> getDetail(Long id) {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long tenantId = currentUser.getTenantId();
        Long userShopId = currentUser.getShopId();

        Review review = reviewMapper.selectById(id);
        if (review == null) {
            return Result.error(ResultCode.NOT_FOUND, "评价不存在");
        }

        // 权限校验
        if ("shop_owner".equals(roleCode) && !review.getShopId().equals(userShopId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该评价");
        }
        if ("tenant_admin".equals(roleCode) && !review.getTenantId().equals(tenantId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该评价");
        }

        ReviewVO vo = convertToVO(review);

        // 获取门店名称
        Shop shop = shopMapper.selectById(review.getShopId());
        if (shop != null) {
            vo.setShopName(shop.getShopName());
        }

        return Result.success(vo);
    }

    @Override
    @Transactional
    public Result<Void> reply(Long id, ReviewReplyRequest request) {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long userShopId = currentUser.getShopId();

        // 权限校验：只有店长可以回复
        if (!"shop_owner".equals(roleCode)) {
            return Result.error(ResultCode.FORBIDDEN, "无权回复评价，仅店长可操作");
        }

        Review review = reviewMapper.selectById(id);
        if (review == null) {
            return Result.error(ResultCode.NOT_FOUND, "评价不存在");
        }

        // 店长只能回复自己门店的评价
        if (!review.getShopId().equals(userShopId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权回复该评价");
        }

        // 检查是否已回复
        if (review.getReplyContent() != null && !review.getReplyContent().isEmpty()) {
            return Result.error(ResultCode.CONFLICT, "该评价已回复，不可重复回复");
        }

        // 更新回复内容
        review.setReplyContent(request.getContent());
        review.setReplyTime(LocalDateTime.now());
        review.setReplyUserId(currentUser.getId());
        reviewMapper.updateById(review);

        return Result.success("回复成功", null);
    }

    @Override
    @Transactional
    public Result<Void> replyWithTemplate(Long id, ReviewReplyTemplateRequest request) {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long userShopId = currentUser.getShopId();
        Long tenantId = currentUser.getTenantId();

        // 权限校验：只有店长可以回复
        if (!"shop_owner".equals(roleCode)) {
            return Result.error(ResultCode.FORBIDDEN, "无权回复评价，仅店长可操作");
        }

        Review review = reviewMapper.selectById(id);
        if (review == null) {
            return Result.error(ResultCode.NOT_FOUND, "评价不存在");
        }

        // 店长只能回复自己门店的评价
        if (!review.getShopId().equals(userShopId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权回复该评价");
        }

        // 检查是否已回复
        if (review.getReplyContent() != null && !review.getReplyContent().isEmpty()) {
            return Result.error(ResultCode.CONFLICT, "该评价已回复，不可重复回复");
        }

        // 获取模板内容
        ReplyTemplate template = replyTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            return Result.error(ResultCode.NOT_FOUND, "模板不存在");
        }

        // 检查模板是否属于同一租户
        if (!template.getTenantId().equals(tenantId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权使用该模板");
        }

        // 更新回复内容
        review.setReplyContent(template.getContent());
        review.setReplyTime(LocalDateTime.now());
        review.setReplyUserId(currentUser.getId());
        reviewMapper.updateById(review);

        return Result.success("回复成功", null);
    }

    @Override
    public Result<ReviewStatisticsVO> getStatistics(ReviewStatisticsDTO dto) {
        SysUser currentUser = UserContext.getUser();
        String roleCode = currentUser.getRoleCode();
        Long tenantId = currentUser.getTenantId();
        Long userShopId = currentUser.getShopId();

        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();

        // 获取当前租户下未删除的门店ID列表（用于过滤已删除门店的评价）
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getTenantId, tenantId);
        List<Shop> validShops = shopMapper.selectList(shopWrapper);
        List<Long> validShopIds = validShops.stream()
                .map(Shop::getId)
                .collect(Collectors.toList());

        if (validShopIds.isEmpty()) {
            ReviewStatisticsVO vo = new ReviewStatisticsVO();
            vo.setTotalCount(0);
            vo.setReplyCount(0);
            vo.setUnReplyCount(0);
            vo.setReplyRate(0.0);
            vo.setAvgStarRating(0.0);
            vo.setSentimentStats(new ReviewStatisticsVO.SentimentStats());
            vo.setPlatformStats(new ReviewStatisticsVO.PlatformStats());
            return Result.success(vo);
        }
        queryWrapper.in(Review::getShopId, validShopIds);

        // 权限控制
        if ("shop_owner".equals(roleCode)) {
            queryWrapper.eq(Review::getShopId, userShopId);
        } else if ("tenant_admin".equals(roleCode)) {
            queryWrapper.eq(Review::getTenantId, tenantId);
            if (dto.getShopId() != null) {
                queryWrapper.eq(Review::getShopId, dto.getShopId());
            }
        }

        List<Review> allReviews = reviewMapper.selectList(queryWrapper);

        ReviewStatisticsVO vo = new ReviewStatisticsVO();

        // 基础统计
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

        // 平均评分
        double avgRating = allReviews.stream()
                .mapToInt(r -> r.getStarRating() != null ? r.getStarRating() : 0)
                .average()
                .orElse(0.0);
        vo.setAvgStarRating(Math.round(avgRating * 10) / 10.0);

        // 情感分布
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

        // 平台分布
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

        return Result.success(vo);
    }

    private ReviewVO convertToVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);

        // 转换标签 JSON
        if (review.getTags() != null && !review.getTags().isEmpty()) {
            try {
                List<String> tagList = objectMapper.readValue(review.getTags(), new TypeReference<List<String>>() {});
                vo.setTags(tagList);
            } catch (Exception e) {
                vo.setTags(new ArrayList<>());
            }
        } else {
            vo.setTags(new ArrayList<>());
        }

        // 计算情感类型
        if (review.getStarRating() != null) {
            if (review.getStarRating() >= 4) {
                vo.setSentimentType(1);
            } else if (review.getStarRating() == 3) {
                vo.setSentimentType(2);
            } else {
                vo.setSentimentType(3);
            }
        }

        // 计算回复状态
        vo.setReplyStatus(review.getReplyContent() != null && !review.getReplyContent().isEmpty() ? 1 : 0);

        // 格式化时间
        if (review.getCreatedAt() != null) {
            vo.setCreatedAt(review.getCreatedAt());
        }
        if (review.getReplyTime() != null) {
            vo.setReplyTime(review.getReplyTime());
        }
        if (review.getOrderTime() != null) {
            vo.setOrderTime(review.getOrderTime());
        }

        return vo;
    }
}
