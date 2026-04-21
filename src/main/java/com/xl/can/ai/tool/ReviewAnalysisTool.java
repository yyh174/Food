package com.xl.can.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.context.UserContext;
import com.xl.can.entity.Review;
import com.xl.can.mapper.ReviewMapper;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReviewAnalysisTool {

    @Autowired
    private ReviewMapper reviewMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(name = "analyze_review_sentiment", description = "分析评价情感，包括好评率、差评率、平均评分等统计。返回总评价数、平均评分、好评/中评/差评的数量和占比、含文字评价数、已回复数及回复率。")
    public ReviewSentimentResult analyzeReviewSentiment(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd HH:mm:ss，默认30天前") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd HH:mm:ss，默认为现在") String endDate) {
        
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : LocalDateTime.now();
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : end.minusDays(30);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Review::getCreatedAt, start)
              .le(Review::getCreatedAt, end);
        
        if ("tenant_admin".equals(roleCode)) {
            wrapper.eq(Review::getTenantId, tenantId);
        } else {
            if (shopId == null) {
                return ReviewSentimentResult.error("您还没有绑定门店");
            }
            wrapper.eq(Review::getShopId, shopId);
        }

        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews == null || reviews.isEmpty()) {
            return ReviewSentimentResult.empty(start.toLocalDate() + " 至 " + end.toLocalDate());
        }

        int totalCount = reviews.size();
        int goodCount = 0;
        int mediumCount = 0;
        int badCount = 0;
        int totalRating = 0;
        int withContentCount = 0;
        int repliedCount = 0;

        for (Review review : reviews) {
            int rating = review.getStarRating() != null ? review.getStarRating() : 0;
            totalRating += rating;

            if (rating >= 4) {
                goodCount++;
            } else if (rating >= 3) {
                mediumCount++;
            } else {
                badCount++;
            }

            if (review.getContent() != null && !review.getContent().isEmpty()) {
                withContentCount++;
            }

            if (review.getReplyContent() != null && !review.getReplyContent().isEmpty()) {
                repliedCount++;
            }
        }

        double avgRating = totalCount > 0 ? (double) totalRating / totalCount : 0;

        ReviewSentimentResult result = new ReviewSentimentResult();
        result.setPeriod(start.toLocalDate() + " 至 " + end.toLocalDate());
        result.setTotalReviews(totalCount);
        result.setAvgRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        result.setGoodReviews(new RatingStats(goodCount, calculateRate(goodCount, totalCount)));
        result.setMediumReviews(new RatingStats(mediumCount, calculateRate(mediumCount, totalCount)));
        result.setBadReviews(new RatingStats(badCount, calculateRate(badCount, totalCount)));
        result.setWithContentCount(withContentCount);
        result.setRepliedCount(repliedCount);
        result.setReplyRate(calculateRate(repliedCount, totalCount));
        return result;
    }

    @Tool(name = "analyze_negative_reviews", description = "分析差评内容，找出顾客投诉最多的问题。返回差评总数、按投诉标签统计的Top问题列表，以及最新差评样例。")
    public NegativeReviewResult analyzeNegativeReviews(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd HH:mm:ss，默认30天前") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd HH:mm:ss，默认为现在") String endDate,
            @ToolParam(description = "评分阈值，低于此评分视为差评，默认为3") Integer ratingThreshold) {
        
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        int threshold = ratingThreshold != null ? ratingThreshold : 3;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : LocalDateTime.now();
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : end.minusDays(30);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Review::getStarRating, threshold)
              .ge(Review::getCreatedAt, start)
              .le(Review::getCreatedAt, end);
        
        if ("tenant_admin".equals(roleCode)) {
            wrapper.eq(Review::getTenantId, tenantId);
        } else {
            if (shopId == null) {
                return NegativeReviewResult.error("您还没有绑定门店");
            }
            wrapper.eq(Review::getShopId, shopId);
        }

        wrapper.orderByDesc(Review::getCreatedAt);
        List<Review> badReviews = reviewMapper.selectList(wrapper);

        if (badReviews == null || badReviews.isEmpty()) {
            return NegativeReviewResult.empty(start.toLocalDate() + " 至 " + end.toLocalDate(), "评分低于" + threshold + "分的差评");
        }

        List<ReviewSample> reviewList = new ArrayList<>();
        Map<String, Integer> tagCount = new HashMap<>();

        for (Review review : badReviews) {
            ReviewSample sample = new ReviewSample();
            sample.setId(review.getId());
            sample.setRating(review.getStarRating());
            sample.setPlatform(review.getPlatform() == 1 ? "美团" : "饿了么");
            sample.setContent(review.getContent());
            sample.setCreatedAt(review.getCreatedAt().toString());
            reviewList.add(sample);

            if (review.getTags() != null && !review.getTags().isEmpty()) {
                String[] tags = review.getTags().replaceAll("[\\[\\]\"]", "").split(",");
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tagCount.put(trimmedTag, tagCount.getOrDefault(trimmedTag, 0) + 1);
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedTags = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        NegativeReviewResult result = new NegativeReviewResult();
        result.setPeriod(start.toLocalDate() + " 至 " + end.toLocalDate());
        result.setTotalNegativeReviews(badReviews.size());
        
        Map<String, Integer> topComplaints = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sortedTags.stream().limit(5).collect(Collectors.toList())) {
            topComplaints.put(entry.getKey(), entry.getValue());
        }
        result.setTopComplaints(topComplaints);
        result.setSampleReviews(reviewList.stream().limit(10).collect(Collectors.toList()));
        return result;
    }

    @Tool(name = "summarize_customer_complaints", description = "总结顾客投诉最多的问题，返回Top N问题列表及典型样例。用于识别主要服务或产品质量问题。")
    public ComplaintSummaryResult summarizeCustomerComplaints(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd HH:mm:ss，默认30天前") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd HH:mm:ss，默认为现在") String endDate,
            @ToolParam(description = "返回的问题数量，默认为5") Integer limit) {
        
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        int topN = limit != null ? limit : 5;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, DATE_FORMATTER) : LocalDateTime.now();
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, DATE_FORMATTER) : end.minusDays(30);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Review::getStarRating, 4)
              .ge(Review::getCreatedAt, start)
              .le(Review::getCreatedAt, end);
        
        if ("tenant_admin".equals(roleCode)) {
            wrapper.eq(Review::getTenantId, tenantId);
        } else {
            if (shopId == null) {
                return ComplaintSummaryResult.error("您还没有绑定门店");
            }
            wrapper.eq(Review::getShopId, shopId);
        }

        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews == null || reviews.isEmpty()) {
            return ComplaintSummaryResult.empty(start.toLocalDate() + " 至 " + end.toLocalDate());
        }

        Map<String, Integer> tagCount = new HashMap<>();
        Map<String, List<String>> tagSamples = new HashMap<>();
        int totalComplaints = 0;

        for (Review review : reviews) {
            if (review.getTags() != null && !review.getTags().isEmpty()) {
                String[] tags = review.getTags().replaceAll("[\\[\\]\"]", "").split(",");
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tagCount.put(trimmedTag, tagCount.getOrDefault(trimmedTag, 0) + 1);
                        tagSamples.computeIfAbsent(trimmedTag, k -> new ArrayList<>()).add(review.getContent());
                        totalComplaints++;
                    }
                }
            }
        }

        final int finalTotalComplaints = totalComplaints;
        List<ComplaintIssue> topComplaints = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    List<String> samples = tagSamples.get(entry.getKey());
                    String sampleText = samples != null && !samples.isEmpty() ? 
                            samples.stream().limit(2).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining("；")) : "无";
                    return new ComplaintIssue(entry.getKey(), entry.getValue(), calculateRate(entry.getValue(), finalTotalComplaints), sampleText);
                })
                .collect(Collectors.toList());

        ComplaintSummaryResult result = new ComplaintSummaryResult();
        result.setPeriod(start.toLocalDate() + " 至 " + end.toLocalDate());
        result.setTotalComplaints(totalComplaints);
        result.setTotalReviewsWithComplaints(reviews.size());
        result.setTopIssues(topComplaints);
        return result;
    }

    @Tool(name = "get_recent_bad_reviews", description = "获取最近的差评列表，用于查看具体投诉内容。返回差评的评分、平台、内容、标签、创建时间及是否已回复。")
    public RecentBadReviewsResult getRecentBadReviews(
            @ToolParam(description = "返回数量，默认为10") Integer limit) {
        
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        int topN = limit != null ? limit : 10;

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Review::getStarRating, 4);
        
        if ("tenant_admin".equals(roleCode)) {
            wrapper.eq(Review::getTenantId, tenantId);
        } else {
            if (shopId == null) {
                return RecentBadReviewsResult.error("您还没有绑定门店");
            }
            wrapper.eq(Review::getShopId, shopId);
        }

        wrapper.orderByDesc(Review::getCreatedAt)
               .last("LIMIT " + topN);

        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews == null || reviews.isEmpty()) {
            return RecentBadReviewsResult.empty();
        }

        List<ReviewSample> reviewList = reviews.stream().map(review -> {
            ReviewSample sample = new ReviewSample();
            sample.setId(review.getId());
            sample.setRating(review.getStarRating());
            sample.setPlatform(review.getPlatform() == 1 ? "美团" : "饿了么");
            sample.setContent(review.getContent());
            sample.setTags(review.getTags());
            sample.setCreatedAt(review.getCreatedAt().toString());
            sample.setHasReplied(review.getReplyContent() != null && !review.getReplyContent().isEmpty());
            return sample;
        }).collect(Collectors.toList());

        RecentBadReviewsResult result = new RecentBadReviewsResult();
        result.setTotal(reviews.size());
        result.setReviews(reviewList);
        return result;
    }

    private String calculateRate(int count, int total) {
        if (total == 0) return "0%";
        double rate = (double) count / total * 100;
        return BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    // ========== 结果类 ==========

    @Data
    public static class ReviewSentimentResult {
        private String period;
        private Integer totalReviews;
        private BigDecimal avgRating;
        private RatingStats goodReviews;
        private RatingStats mediumReviews;
        private RatingStats badReviews;
        private Integer withContentCount;
        private Integer repliedCount;
        private String replyRate;
        private String error;

        public static ReviewSentimentResult empty(String period) {
            ReviewSentimentResult r = new ReviewSentimentResult();
            r.setPeriod(period);
            r.setTotalReviews(0);
            r.setAvgRating(BigDecimal.ZERO);
            return r;
        }

        public static ReviewSentimentResult error(String message) {
            ReviewSentimentResult r = new ReviewSentimentResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class RatingStats {
        private Integer count;
        private String rate;
        public RatingStats() {}
        public RatingStats(Integer count, String rate) { this.count = count; this.rate = rate; }
    }

    @Data
    public static class NegativeReviewResult {
        private String period;
        private Integer totalNegativeReviews;
        private Map<String, Integer> topComplaints;
        private List<ReviewSample> sampleReviews;
        private String error;

        public static NegativeReviewResult empty(String period, String reason) {
            NegativeReviewResult r = new NegativeReviewResult();
            r.setPeriod(period);
            r.setTotalNegativeReviews(0);
            return r;
        }

        public static NegativeReviewResult error(String message) {
            NegativeReviewResult r = new NegativeReviewResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class ReviewSample {
        private Long id;
        private Integer rating;
        private String platform;
        private String content;
        private String tags;
        private String createdAt;
        private Boolean hasReplied;
    }

    @Data
    public static class ComplaintSummaryResult {
        private String period;
        private Integer totalComplaints;
        private Integer totalReviewsWithComplaints;
        private List<ComplaintIssue> topIssues;
        private String error;

        public static ComplaintSummaryResult empty(String period) {
            ComplaintSummaryResult r = new ComplaintSummaryResult();
            r.setPeriod(period);
            r.setTotalComplaints(0);
            r.setTotalReviewsWithComplaints(0);
            return r;
        }

        public static ComplaintSummaryResult error(String message) {
            ComplaintSummaryResult r = new ComplaintSummaryResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class ComplaintIssue {
        private String issue;
        private Integer count;
        private String percentage;
        private String sample;
        public ComplaintIssue() {}
        public ComplaintIssue(String issue, Integer count, String percentage, String sample) {
            this.issue = issue; this.count = count; this.percentage = percentage; this.sample = sample;
        }
    }

    @Data
    public static class RecentBadReviewsResult {
        private Integer total;
        private List<ReviewSample> reviews;
        private String error;

        public static RecentBadReviewsResult empty() {
            RecentBadReviewsResult r = new RecentBadReviewsResult();
            r.setTotal(0);
            return r;
        }

        public static RecentBadReviewsResult error(String message) {
            RecentBadReviewsResult r = new RecentBadReviewsResult();
            r.setError(message);
            return r;
        }
    }
}
