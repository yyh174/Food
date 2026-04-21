package com.xl.can.vo;

import lombok.Data;

@Data
public class TicketStatisticsVO {
    private Integer totalCount;
    private Integer pendingCount;
    private Integer processingCount;
    private Integer pendingVerifyCount;
    private Integer archivedCount;
    private Integer overdueCount;
    private PriorityStats priorityStats;
    private TypeStats typeStats;

    @Data
    public static class PriorityStats {
        private Integer high;
        private Integer medium;
        private Integer low;
    }

    @Data
    public static class TypeStats {
        private Integer negativeReview;
        private Integer complaint;
        private Integer metric;
        private Integer custom;
    }
}
