package com.xl.can.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xl.can.config.ReviewMonitorProperties;
import com.xl.can.entity.Review;
import com.xl.can.entity.Ticket;
import com.xl.can.entity.TicketType;
import com.xl.can.mapper.ReviewMapper;
import com.xl.can.mapper.TicketMapper;
import com.xl.can.mapper.TicketTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewMonitorTask {

    private final ReviewMapper reviewMapper;
    private final TicketMapper ticketMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final ReviewMonitorProperties properties;

    private static final DateTimeFormatter TICKET_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Scheduled(cron = "${review.monitor.cron:0 */5 * * * ?}")
    public void scanAndCreateTicketsForBadReviews() {
        if (!properties.isEnabled()) {
            log.debug("差评监控已禁用，跳过扫描");
            return;
        }

        if (!properties.isAutoCreateTicket()) {
            log.debug("自动创建工单已禁用，跳过扫描");
            return;
        }

        log.info("开始扫描差评并创建工单...");

        try {
            LocalDateTime scanTime = LocalDateTime.now();

            LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.le(Review::getStarRating, properties.getStarThreshold())
                    .isNull(Review::getReplyContent)
                    .ge(Review::getUpdatedAt, scanTime.minusMinutes(properties.getScanWindowMinutes()))
                    .orderByDesc(Review::getUpdatedAt);

            List<Review> badReviews = reviewMapper.selectList(queryWrapper);

            if (badReviews.isEmpty()) {
                log.info("本次扫描未发现新的差评");
                return;
            }

            log.info("发现 {} 条待处理的差评", badReviews.size());

            for (Review review : badReviews) {
                processBadReview(review);
            }

        } catch (Exception e) {
            log.error("扫描差评并创建工单失败", e);
        }
    }

    private void processBadReview(Review review) {
        try {
            if (ticketMapper.selectCount(
                    new LambdaQueryWrapper<Ticket>()
                            .eq(Ticket::getReviewId, review.getId())
                            .eq(Ticket::getDeletedAt, 0)
            ) > 0) {
                log.info("评价 {} 已有关联的工单，跳过", review.getId());
                return;
            }

            TicketType defaultType = ticketTypeMapper.selectOne(
                    new LambdaQueryWrapper<TicketType>()
                            .eq(TicketType::getTenantId, review.getTenantId())
                            .eq(TicketType::getType, "negative_review")
                            .eq(TicketType::getSupportReview, true)
                            .eq(TicketType::getStatus, 1)
                            .orderByAsc(TicketType::getSortOrder)
                            .last("LIMIT 1")
            );

            if (defaultType == null) {
                defaultType = ticketTypeMapper.selectOne(
                        new LambdaQueryWrapper<TicketType>()
                                .eq(TicketType::getTenantId, review.getTenantId())
                                .eq(TicketType::getIsDefault, true)
                                .eq(TicketType::getStatus, 1)
                                .last("LIMIT 1")
                );
            }

            Ticket ticket = new Ticket();
            ticket.setTicketNo(generateTicketNo());
            ticket.setTypeId(defaultType != null ? defaultType.getId() : null);
            ticket.setStatus(0);
            ticket.setCategory(0);
            ticket.setShopId(review.getShopId());
            ticket.setTenantId(review.getTenantId());
            ticket.setTitle("差评提醒 - " + review.getPlatformOrderId());
            ticket.setDescription(buildTicketDescription(review));
            ticket.setReviewId(review.getId());
            ticket.setPriority(determinePriority(review.getStarRating()));
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());
            ticket.setDeletedAt(0);

            ticketMapper.insert(ticket);

            LambdaUpdateWrapper<Review> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Review::getId, review.getId())
                    .set(Review::getTags, mergeTags(review.getTags(), "auto_ticket"));
            reviewMapper.update(null, updateWrapper);

            log.info("为评价 {} 创建工单成功，工单号: {}", review.getId(), ticket.getTicketNo());

        } catch (Exception e) {
            log.error("处理差评 {} 创建工单失败", review.getId(), e);
        }
    }

    private String generateTicketNo() {
        return "TK" + LocalDateTime.now().format(TICKET_NO_FORMATTER) + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String buildTicketDescription(Review review) {
        StringBuilder sb = new StringBuilder();
        sb.append("【差评提醒】\n\n");
        sb.append("平台: ").append(getPlatformName(review.getPlatform())).append("\n");
        sb.append("订单号: ").append(review.getPlatformOrderId()).append("\n");
        sb.append("评分: ");
        for (int i = 0; i < 5; i++) {
            sb.append(i < review.getStarRating() ? "★" : "☆");
        }
        sb.append(" (").append(review.getStarRating()).append("星)\n\n");
        sb.append("评价内容:\n").append(review.getContent()).append("\n\n");
        sb.append("评价时间: ").append(review.getOrderTime()).append("\n");
        return sb.toString();
    }

    private String determinePriority(int starRating) {
        if (starRating <= 1) {
            return "high";
        } else if (starRating == 2) {
            return "medium";
        } else {
            return "low";
        }
    }

    private String getPlatformName(Integer platform) {
        if (platform == null) return "未知";
        return switch (platform) {
            case 1 -> "美团";
            case 2 -> "饿了么";
            default -> "未知";
        };
    }

    private String mergeTags(String existingTags, String newTag) {
        if (existingTags == null || existingTags.isEmpty()) {
            return newTag;
        }
        if (existingTags.contains(newTag)) {
            return existingTags;
        }
        return existingTags + "," + newTag;
    }
}
