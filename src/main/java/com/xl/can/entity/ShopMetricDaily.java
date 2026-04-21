package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("shop_metric_daily")
public class ShopMetricDaily {
    @TableId
    private Long shopId;
    private LocalDate statDate;
    private Integer meituanOrderCount;
    private BigDecimal meituanOrderAmount;
    private Integer meituanReviewCount;
    private BigDecimal meituanAvgRating;
    private Integer meituanNegativeCount;
    private Integer eleOrderCount;
    private BigDecimal eleOrderAmount;
    private Integer eleReviewCount;
    private BigDecimal eleAvgRating;
    private Integer eleNegativeCount;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private Integer totalReviewCount;
    private BigDecimal totalAvgRating;
    private BigDecimal goodReviewRate;
}
