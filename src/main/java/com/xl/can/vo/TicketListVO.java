package com.xl.can.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class TicketListVO {
    private Long id;
    private String title;
    private Long typeId;
    private String typeName;
    private Long shopId;
    private String shopName;
    private Integer status;
    private String statusName;
    private String priority;      // 数据库存储的原始优先级
    private String priorityText; // 动态计算的优先级显示文本
    private String priorityColor; // 动态计算的优先级颜色
    private String deadline;
    private Long creatorId;
    private String creatorName;
    private Long assigneeId;
    private String assigneeName;
    private Long reviewId;
    private Integer reviewStar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 根据截止时间动态计算优先级
     */
    public void calculateDynamicPriority() {
        if (deadline == null) {
            this.priorityText = "低";
            this.priorityColor = "#52c41a";
            return;
        }
        try {
            LocalDateTime deadlineTime = LocalDateTime.parse(deadline, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), deadlineTime);
            if (hours <= 0) {
                this.priorityText = "紧急";
                this.priorityColor = "#ff4d4f";
            } else if (hours <= 24) {
                this.priorityText = "高";
                this.priorityColor = "#ff4d4f";
            } else if (hours <= 72) {
                this.priorityText = "中";
                this.priorityColor = "#faad14";
            } else {
                this.priorityText = "低";
                this.priorityColor = "#52c41a";
            }
        } catch (Exception e) {
            this.priorityText = "低";
            this.priorityColor = "#52c41a";
        }
    }
}
