package com.xl.can.vo;

import lombok.Data;

@Data
public class ReviewStatisticsVO {

    private Integer totalCount;

    private Integer replyCount;

    private Integer unReplyCount;

    private Double replyRate;

    private Double avgStarRating;

    private SentimentStats sentimentStats;

    private PlatformStats platformStats;

    @Data
    public static class SentimentStats {
        private Integer positive;
        private Integer neutral;
        private Integer negative;
    }

    @Data
    public static class PlatformStats {
        private Integer meituan;
        private Integer ele;
    }
}
