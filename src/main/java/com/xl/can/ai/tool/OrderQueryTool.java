package com.xl.can.ai.tool;

import com.xl.can.context.UserContext;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class OrderQueryTool {

    @Tool(name = "get_order_statistics", description = "Get order statistics.")
    public OrderStatisticsResult getOrderStatistics(
            @ToolParam(description = "Start date, format: yyyy-MM-dd") String startDate,
            @ToolParam(description = "End date, format: yyyy-MM-dd") String endDate) {
        
        Long tenantId = UserContext.getTenantId();
        String roleCode = UserContext.getRoleCode();

        if (tenantId == null) {
            return OrderStatisticsResult.error("Unable to get tenant info");
        }

        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);
        if (end == null) end = LocalDate.now();
        if (start == null) start = end.minusDays(30);

        String scope = "tenant_admin".equals(roleCode) ? "All shops" : "Current shop";

        OrderStatisticsResult result = new OrderStatisticsResult();
        result.setPeriod(start + " to " + end);
        result.setScope(scope);
        result.setNote("Detailed order data needs to be retrieved from platform API");
        return result;
    }

    @Tool(name = "analyze_customer_flow", description = "Analyze customer flow data.")
    public CustomerFlowResult analyzeCustomerFlow(
            @ToolParam(description = "Start date, format: yyyy-MM-dd") String startDate,
            @ToolParam(description = "End date, format: yyyy-MM-dd") String endDate) {
        
        Long tenantId = UserContext.getTenantId();
        String roleCode = UserContext.getRoleCode();

        if (tenantId == null) {
            return CustomerFlowResult.error("Unable to get tenant info");
        }

        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);
        if (end == null) end = LocalDate.now();
        if (start == null) start = end.minusDays(30);

        String scope = "tenant_admin".equals(roleCode) ? "All shops" : "Current shop";

        CustomerFlowResult result = new CustomerFlowResult();
        result.setPeriod(start + " to " + end);
        result.setScope(scope);
        result.setNote("Customer flow is estimated based on order volume");
        return result;
    }

    @Tool(name = "analyze_sales_drop_reasons", description = "Analyze possible reasons for sales decline.")
    public SalesDropAnalysisResult analyzeSalesDropReasons(
            @ToolParam(description = "Analysis start date, format: yyyy-MM-dd") String startDate,
            @ToolParam(description = "Analysis end date, format: yyyy-MM-dd") String endDate) {
        
        Long tenantId = UserContext.getTenantId();
        String roleCode = UserContext.getRoleCode();

        if (tenantId == null) {
            return SalesDropAnalysisResult.error("Unable to get tenant info");
        }

        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);
        if (end == null) end = LocalDate.now();
        if (start == null) start = end.minusDays(30);

        SalesDropAnalysisResult result = new SalesDropAnalysisResult();
        result.setAnalysisPeriod(start + " to " + end);
        result.setScope("tenant_admin".equals(roleCode) ? "All shops" : "Current shop");
        
        List<String> externalFactors = Arrays.asList(
                "Weather conditions",
                "Holiday effects",
                "Competitor promotions",
                "Platform traffic fluctuations"
        );
        
        List<String> internalFactors = Arrays.asList(
                "Declining ratings",
                "More negative reviews",
                "Reduced promotional activities",
                "Menu not updated timely"
        );
        
        List<String> suggestions = Arrays.asList(
                "Strengthen customer service",
                "Monitor weather changes",
                "Participate in platform activities",
                "Improve food quality and service"
        );
        
        result.setExternalFactors(externalFactors);
        result.setInternalFactors(internalFactors);
        result.setSuggestions(suggestions);
        return result;
    }

    @Tool(name = "get_shop_info", description = "Get current user shop info.")
    public ShopInfoResult getShopInfo() {
        Long tenantId = UserContext.getTenantId();
        Long shopId = UserContext.getShopId();
        String roleCode = UserContext.getRoleCode();

        if (tenantId == null) {
            return ShopInfoResult.error("Unable to get tenant info");
        }

        ShopInfoResult result = new ShopInfoResult();
        result.setTenantId(tenantId);
        result.setShopId(shopId);
        result.setRoleCode(roleCode);
        result.setRoleName("tenant_admin".equals(roleCode) ? "Tenant Admin" : "Shop Manager");
        return result;
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
    public static class OrderStatisticsResult {
        private String period;
        private String scope;
        private String note;
        private String suggestion;
        private String error;

        public static OrderStatisticsResult error(String message) {
            OrderStatisticsResult r = new OrderStatisticsResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class CustomerFlowResult {
        private String period;
        private String scope;
        private String note;
        private String suggestion;
        private String error;

        public static CustomerFlowResult error(String message) {
            CustomerFlowResult r = new CustomerFlowResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class SalesDropAnalysisResult {
        private String analysisPeriod;
        private String scope;
        private List<String> externalFactors;
        private List<String> internalFactors;
        private List<String> suggestions;
        private String error;

        public static SalesDropAnalysisResult error(String message) {
            SalesDropAnalysisResult r = new SalesDropAnalysisResult();
            r.setError(message);
            return r;
        }
    }

    @Data
    public static class ShopInfoResult {
        private Long tenantId;
        private Long shopId;
        private String roleCode;
        private String roleName;
        private String error;

        public static ShopInfoResult error(String message) {
            ShopInfoResult r = new ShopInfoResult();
            r.setError(message);
            return r;
        }
    }
}