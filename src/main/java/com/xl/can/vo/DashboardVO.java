package com.xl.can.vo;

import lombok.Data;

@Data
public class DashboardVO {

    private String roleCode;

    private StatCards statCards;

    private ReviewStatisticsVO reviewStatistics;

    private TicketStatisticsVO ticketStatistics;

    @Data
    public static class StatCards {
        private Integer shopCount;
        private Integer userCount;
        private Integer todayReviewCount;
        private Integer pendingTicketCount;
        private Integer todayTicketCount;
        private Integer pendingReviewCount;
        private Integer repliedReviewCount;
        private Integer totalReviewCount;
    }
}
