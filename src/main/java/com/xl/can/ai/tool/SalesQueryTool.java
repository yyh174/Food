package com.xl.can.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xl.can.context.UserContext;
import com.xl.can.entity.Shop;
import com.xl.can.entity.ShopMetricDaily;
import com.xl.can.mapper.ShopMetricDailyMapper;
import com.xl.can.mapper.ShopMapper;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SalesQueryTool {

    @Autowired
    private ShopMetricDailyMapper shopMetricDailyMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Tool(name = "query_sales_data", description = "Query sales data for date range.")
    public SalesDataResult querySalesData(
            @ToolParam(description = "Start date, format: yyyy-MM-dd") String startDate,
            @ToolParam(description = "End date, format: yyyy-MM-dd") String endDate) {

        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);

        if (end == null) end = LocalDate.now();
        if (start == null) start = end.minusDays(30);

        List<ShopMetricDaily> dataList;
        if ("tenant_admin".equals(roleCode)) {
            List<Long> shopIds = getShopIdsByTenantId(tenantId);
            if (shopIds.isEmpty()) {
                return SalesDataResult.empty(start + " to " + end, "No shops under this tenant");
            }
            dataList = shopMetricDailyMapper.findByShopIdsAndDateRange(shopIds, start, end);
        } else {
            if (shopId == null) {
                return SalesDataResult.error("No shop bound");
            }
            dataList = shopMetricDailyMapper.findByShopIdAndDateRange(shopId, start, end);
        }

        if (dataList == null || dataList.isEmpty()) {
            return SalesDataResult.empty(start + " to " + end, null);
        }

        int totalOrderCount = 0;
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        int totalReviewCount = 0;
        int negativeCount = 0;
        List<DailySalesData> dailyList = new ArrayList<>();

        for (ShopMetricDaily data : dataList) {
            totalOrderCount += (data.getTotalOrderCount() != null ? data.getTotalOrderCount() : 0);
            totalOrderAmount = totalOrderAmount.add(data.getTotalOrderAmount() != null ? data.getTotalOrderAmount() : BigDecimal.ZERO);
            totalReviewCount += (data.getTotalReviewCount() != null ? data.getTotalReviewCount() : 0);
            negativeCount += (data.getMeituanNegativeCount() != null ? data.getMeituanNegativeCount() : 0);
            negativeCount += (data.getEleNegativeCount() != null ? data.getEleNegativeCount() : 0);

            DailySalesData daily = new DailySalesData();
            daily.setDate(data.getStatDate().toString());
            daily.setOrderCount(data.getTotalOrderCount());
            daily.setOrderAmount(data.getTotalOrderAmount());
            daily.setReviewCount(data.getTotalReviewCount());
            daily.setAvgRating(data.getTotalAvgRating());
            dailyList.add(daily);
        }

        BigDecimal avgOrderAmount = totalOrderCount > 0 ? totalOrderAmount.divide(BigDecimal.valueOf(totalOrderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        SalesDataResult result = new SalesDataResult();
        result.setPeriod(start + " to " + end);
        result.setTotalOrderCount(totalOrderCount);
        result.setTotalOrderAmount(totalOrderAmount.setScale(2, RoundingMode.HALF_UP));
        result.setAvgOrderAmount(avgOrderAmount);
        result.setTotalReviewCount(totalReviewCount);
        result.setNegativeReviewCount(negativeCount);
        result.setDailyData(dailyList);
        return result;
    }

    @Tool(name = "compare_sales_by_period", description = "Compare sales between two periods.")
    public CompareSalesResult compareSalesByPeriod(
            @ToolParam(description = "Period 1 start date, yyyy-MM-dd") String period1Start,
            @ToolParam(description = "Period 1 end date, yyyy-MM-dd") String period1End,
            @ToolParam(description = "Period 2 start date, yyyy-MM-dd") String period2Start,
            @ToolParam(description = "Period 2 end date, yyyy-MM-dd") String period2End) {

        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        LocalDate p1Start = parseDate(period1Start);
        LocalDate p1End = parseDate(period1End);
        LocalDate p2Start = parseDate(period2Start);
        LocalDate p2End = parseDate(period2End);

        if (p1Start == null || p1End == null || p2Start == null || p2End == null) {
            return CompareSalesResult.error("Date format error, use yyyy-MM-dd");
        }

        List<ShopMetricDaily> data1, data2;
        if ("tenant_admin".equals(roleCode)) {
            List<Long> shopIds = getShopIdsByTenantId(tenantId);
            if (shopIds.isEmpty()) {
                return CompareSalesResult.error("No shops under this tenant");
            }
            data1 = shopMetricDailyMapper.findByShopIdsAndDateRange(shopIds, p1Start, p1End);
            data2 = shopMetricDailyMapper.findByShopIdsAndDateRange(shopIds, p2Start, p2End);
        } else {
            if (shopId == null) {
                return CompareSalesResult.error("No shop bound");
            }
            data1 = shopMetricDailyMapper.findByShopIdAndDateRange(shopId, p1Start, p1End);
            data2 = shopMetricDailyMapper.findByShopIdAndDateRange(shopId, p2Start, p2End);
        }

        Map<String, Object> stats1 = calculateStats(data1);
        Map<String, Object> stats2 = calculateStats(data2);

        CompareSalesResult result = new CompareSalesResult();
        result.setPeriod1(new PeriodStats(p1Start + " to " + p1End,
                (Integer) stats1.get("orderCount"),
                (BigDecimal) stats1.get("orderAmount"),
                (BigDecimal) stats1.get("avgOrderAmount")));
        result.setPeriod2(new PeriodStats(p2Start + " to " + p2End,
                (Integer) stats2.get("orderCount"),
                (BigDecimal) stats2.get("orderAmount"),
                (BigDecimal) stats2.get("avgOrderAmount")));
        result.setChange(calculateChange(stats1, stats2));
        return result;
    }

    @Tool(name = "analyze_weekday_weekend", description = "Analyze weekday vs weekend sales.")
    public WeekdayWeekendResult analyzeWeekdayWeekend(
            @ToolParam(description = "Start date, yyyy-MM-dd") String startDate,
            @ToolParam(description = "End date, yyyy-MM-dd") String endDate) {

        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null) {
            return WeekdayWeekendResult.error("Date format error, use yyyy-MM-dd");
        }

        List<ShopMetricDaily> dataList;
        if ("tenant_admin".equals(roleCode)) {
            List<Long> shopIds = getShopIdsByTenantId(tenantId);
            if (shopIds.isEmpty()) {
                return WeekdayWeekendResult.error("No shops under this tenant");
            }
            dataList = shopMetricDailyMapper.findByShopIdsAndDateRange(shopIds, start, end);
        } else {
            if (shopId == null) {
                return WeekdayWeekendResult.error("No shop bound");
            }
            dataList = shopMetricDailyMapper.findByShopIdAndDateRange(shopId, start, end);
        }

        List<ShopMetricDaily> weekdayData = new ArrayList<>();
        List<ShopMetricDaily> weekendData = new ArrayList<>();

        for (ShopMetricDaily data : dataList) {
            DayOfWeek dow = data.getStatDate().getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendData.add(data);
            } else {
                weekdayData.add(data);
            }
        }

        Map<String, Object> weekdayStats = calculateStats(weekdayData);
        Map<String, Object> weekendStats = calculateStats(weekendData);

        WeekdayWeekendResult result = new WeekdayWeekendResult();
        result.setPeriod(start + " to " + end);
        result.setWeekday(new DayTypeStats(weekdayData.size(),
                (Integer) weekdayStats.get("orderCount"),
                (BigDecimal) weekdayStats.get("avgOrderCount"),
                (BigDecimal) weekdayStats.get("orderAmount")));
        result.setWeekend(new DayTypeStats(weekendData.size(),
                (Integer) weekendStats.get("orderCount"),
                (BigDecimal) weekendStats.get("avgOrderCount"),
                (BigDecimal) weekendStats.get("orderAmount")));

        BigDecimal weekdayAvg = (BigDecimal) weekdayStats.get("avgOrderCount");
        BigDecimal weekendAvg = (BigDecimal) weekendStats.get("avgOrderCount");
        if (weekdayAvg != null && weekendAvg != null && weekdayAvg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal changeRate = weekendAvg.subtract(weekdayAvg).divide(weekdayAvg, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.setWeekendVsWeekdayChange(changeRate.setScale(1, RoundingMode.HALF_UP) + "%");
        }
        return result;
    }

    private List<Long> getShopIdsByTenantId(Long tenantId) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getTenantId, tenantId)
               .eq(Shop::getDeletedAt, 0);
        return shopMapper.selectList(wrapper).stream()
                .map(Shop::getId)
                .collect(Collectors.toList());
    }

    private Map<String, Object> calculateStats(List<ShopMetricDaily> dataList) {
        int orderCount = 0;
        BigDecimal orderAmount = BigDecimal.ZERO;
        int days = dataList.size();

        for (ShopMetricDaily data : dataList) {
            orderCount += (data.getTotalOrderCount() != null ? data.getTotalOrderCount() : 0);
            orderAmount = orderAmount.add(data.getTotalOrderAmount() != null ? data.getTotalOrderAmount() : BigDecimal.ZERO);
        }

        BigDecimal avgOrderCount = days > 0 ? BigDecimal.valueOf(orderCount).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgOrderAmount = orderCount > 0 ? orderAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("orderCount", orderCount);
        stats.put("orderAmount", orderAmount.setScale(2, RoundingMode.HALF_UP));
        stats.put("avgOrderCount", avgOrderCount);
        stats.put("avgOrderAmount", avgOrderAmount);
        return stats;
    }

    private Map<String, String> calculateChange(Map<String, Object> stats1, Map<String, Object> stats2) {
        Map<String, String> change = new LinkedHashMap<>();

        int orderCount1 = (Integer) stats1.get("orderCount");
        int orderCount2 = (Integer) stats2.get("orderCount");
        BigDecimal amount1 = (BigDecimal) stats1.get("orderAmount");
        BigDecimal amount2 = (BigDecimal) stats2.get("orderAmount");

        if (orderCount1 > 0) {
            BigDecimal orderChange = BigDecimal.valueOf(orderCount2 - orderCount1)
                    .divide(BigDecimal.valueOf(orderCount1), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            change.put("orderCountChange", orderChange.setScale(1, RoundingMode.HALF_UP) + "%");
        }

        if (amount1.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal amountChange = amount2.subtract(amount1).divide(amount1, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            change.put("orderAmountChange", amountChange.setScale(1, RoundingMode.HALF_UP) + "%");
        }

        return change;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        dateStr = dateStr.trim();

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {}

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        } catch (Exception e) {}

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } catch (Exception e) {}

        return null;
    }

    @Data
    public static class SalesDataResult {
        private String period;
        private Integer totalOrderCount;
        private BigDecimal totalOrderAmount;
        private BigDecimal avgOrderAmount;
        private Integer totalReviewCount;
        private Integer negativeReviewCount;
        private List<DailySalesData> dailyData;
        private String error;
        private Boolean empty;

        public static SalesDataResult empty(String period, String reason) {
            SalesDataResult r = new SalesDataResult();
            r.setPeriod(period);
            r.setEmpty(true);
            r.setTotalOrderCount(0);
            r.setTotalOrderAmount(BigDecimal.ZERO);
            r.setError(reason);
            return r;
        }

        public static SalesDataResult error(String message) {
            SalesDataResult r = new SalesDataResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class DailySalesData {
        private String date;
        private Integer orderCount;
        private BigDecimal orderAmount;
        private Integer reviewCount;
        private BigDecimal avgRating;
    }

    @Data
    public static class CompareSalesResult {
        private PeriodStats period1;
        private PeriodStats period2;
        private Map<String, String> change;
        private String error;

        public static CompareSalesResult error(String message) {
            CompareSalesResult r = new CompareSalesResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class PeriodStats {
        private String range;
        private Integer orderCount;
        private BigDecimal orderAmount;
        private BigDecimal avgOrderAmount;

        public PeriodStats() {}
        public PeriodStats(String range, Integer orderCount, BigDecimal orderAmount, BigDecimal avgOrderAmount) {
            this.range = range;
            this.orderCount = orderCount;
            this.orderAmount = orderAmount;
            this.avgOrderAmount = avgOrderAmount;
        }
    }

    @Data
    public static class WeekdayWeekendResult {
        private String period;
        private DayTypeStats weekday;
        private DayTypeStats weekend;
        private String weekendVsWeekdayChange;
        private String error;

        public static WeekdayWeekendResult error(String message) {
            WeekdayWeekendResult r = new WeekdayWeekendResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class DayTypeStats {
        private Integer days;
        private Integer totalOrderCount;
        private BigDecimal avgOrderCount;
        private BigDecimal totalAmount;

        public DayTypeStats() {}
        public DayTypeStats(Integer days, Integer totalOrderCount, BigDecimal avgOrderCount, BigDecimal totalAmount) {
            this.days = days;
            this.totalOrderCount = totalOrderCount;
            this.avgOrderCount = avgOrderCount;
            this.totalAmount = totalAmount;
        }
    }
}