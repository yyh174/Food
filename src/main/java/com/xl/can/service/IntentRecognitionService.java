package com.xl.can.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI 聊天意图识别服务
 * 在调用工具前先识别用户意图，决定需要调用哪些工具
 */
@Service
public class IntentRecognitionService {

    /**
     * 意图类型枚举
     */
    public enum Intent {
        /** 销量相关 */
        SALES,
        /** 评价相关 */
        REVIEW,
        /** 客流相关 */
        CUSTOMER_FLOW,
        /** 订单相关 */
        ORDER,
        /** 门店信息 */
        SHOP_INFO,
        /** 通用问题，无需工具 */
        GENERAL
    }

    /**
     * 意图与工具的映射
     */
    private static final Map<Intent, Set<String>> INTENT_TOOLS_MAP = new EnumMap<>(Intent.class);

    static {
        INTENT_TOOLS_MAP.put(Intent.SALES, Set.of(
                "SalesQueryTool"
        ));
        INTENT_TOOLS_MAP.put(Intent.REVIEW, Set.of(
                "ReviewAnalysisTool"
        ));
        INTENT_TOOLS_MAP.put(Intent.CUSTOMER_FLOW, Set.of(
                "OrderQueryTool"
        ));
        INTENT_TOOLS_MAP.put(Intent.ORDER, Set.of(
                "OrderQueryTool"
        ));
        INTENT_TOOLS_MAP.put(Intent.SHOP_INFO, Set.of(
                "OrderQueryTool"
        ));
        INTENT_TOOLS_MAP.put(Intent.GENERAL, Set.of());
    }

    /**
     * 识别用户意图
     * @param userInput 用户输入
     * @return 识别出的意图列表
     */
    public List<Intent> recognize(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return List.of(Intent.GENERAL);
        }

        String input = userInput.toLowerCase().trim();

        // 问候语/闲聊 - 不需要工具
        if (isGreeting(input)) {
            return List.of(Intent.GENERAL);
        }

        Set<Intent> intents = new LinkedHashSet<>();

        // 销量相关关键词
        if (containsAny(input, "销量", "销售额", "卖了多少", "订单量", "日均", "环比", "同比",
                "下降", "上升", "增长", "减少", "变化", "趋势", "对比", "比较",
                "上周", "本周", "上月", "本月", "昨天", "今天", "这段时间",
                "周末", "工作日", "周一到周五", "周末", "工作日差异", "生意")) {
            intents.add(Intent.SALES);
        }

        // 评价相关关键词
        if (containsAny(input, "评价", "好评", "差评", "投诉", "评分", "星级", "星数",
                "顾客反馈", "顾客说", "评论", "吐槽", "抱怨", "不满", "回复率",
                "负面评价", "正面评价", "中评", "口味", "服务态度", "包装", "速度")) {
            intents.add(Intent.REVIEW);
        }

        // 客流相关关键词
        if (containsAny(input, "客流", "人流量", "多少人", "来客", "到店", "进店",
                "人少", "高峰", "低谷", "时段", "几点")) {
            intents.add(Intent.CUSTOMER_FLOW);
        }

        // 订单相关关键词
        if (containsAny(input, "订单", "外卖", "美团", "饿了么", "平台", "各平台",
                "订单数", "接单", "拒单", "取消")) {
            intents.add(Intent.ORDER);
        }

        // 门店信息
        if (containsAny(input, "门店", "店信息", "店铺", "我的店", "当前", "基本信息")) {
            intents.add(Intent.SHOP_INFO);
        }

        // 如果没有匹配到具体意图，检查是否需要工具
        if (intents.isEmpty()) {
            // 检查是否可能是数据分析相关
            if (containsAny(input, "分析", "数据", "统计", "报告", "为什么", "原因", "建议", "诊断")) {
                // 默认当作销量分析
                intents.add(Intent.SALES);
                intents.add(Intent.REVIEW);
            } else {
                // 通用问题
                intents.add(Intent.GENERAL);
            }
        }

        return new ArrayList<>(intents);
    }

    /**
     * 检查是否为问候语/闲聊
     */
    private boolean isGreeting(String input) {
        // 简单问候
        if (input.matches("^(你好|您好|嗨|hi|hello|hey|hi there|嘿|在吗|在不在|帮忙).*$")) {
            return true;
        }
        // 纯问候或简单响应
        if (input.matches("^(你好|您好|嗨|hi|hello|hey|嘿)$")) {
            return true;
        }
        // 询问AI身份
        if (input.matches("^(你是谁|你是|你是干什么的|你能做什么|你会什么).*$")) {
            return true;
        }
        return false;
    }

    /**
     * 根据意图获取需要加载的工具类名
     * @param intents 意图列表
     * @return 需要加载的工具类名集合
     */
    public Set<String> getRequiredToolClasses(List<Intent> intents) {
        // 如果包含通用意图，不加载任何工具
        if (intents.contains(Intent.GENERAL) && intents.size() == 1) {
            return Set.of();
        }

        Set<String> tools = new HashSet<>();
        for (Intent intent : intents) {
            if (intent != Intent.GENERAL) {
                tools.addAll(INTENT_TOOLS_MAP.getOrDefault(intent, Set.of()));
            }
        }
        return tools;
    }

    /**
     * 检查输入是否包含任意一个关键词
     */
    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取意图的友好描述
     */
    public String getIntentDescription(Intent intent) {
        return switch (intent) {
            case SALES -> "销量数据分析";
            case REVIEW -> "评价分析";
            case CUSTOMER_FLOW -> "客流分析";
            case ORDER -> "订单统计";
            case SHOP_INFO -> "门店信息";
            case GENERAL -> "通用问题";
        };
    }

    /**
     * 获取意图的中文名称
     */
    public String getIntentName(Intent intent) {
        return switch (intent) {
            case SALES -> "销量";
            case REVIEW -> "评价";
            case CUSTOMER_FLOW -> "客流";
            case ORDER -> "订单";
            case SHOP_INFO -> "门店";
            case GENERAL -> "通用";
        };
    }
}
