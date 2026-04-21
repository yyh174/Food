package com.xl.can.controller;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.dto.ReviewListDTO;
import com.xl.can.dto.ReviewReplyRequest;
import com.xl.can.dto.ReviewReplyTemplateRequest;
import com.xl.can.dto.ReviewStatisticsDTO;
import com.xl.can.service.ReviewService;
import com.xl.can.vo.ReviewStatisticsVO;
import com.xl.can.vo.ReviewVO;
import com.xl.can.vo.ReviewForTicketVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private com.xl.can.service.TicketService ticketService;

    @GetMapping
    public Result<PageResult<ReviewVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer platform,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer replyStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        ReviewListDTO dto = new ReviewListDTO();
        dto.setPage(page);
        dto.setPageSize(pageSize);
        dto.setPlatform(platform);
        dto.setShopId(shopId);
        dto.setReplyStatus(replyStatus);
        dto.setKeyword(keyword);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);

        return reviewService.pageList(dto);
    }

    @GetMapping("/{id}")
    public Result<ReviewVO> detail(@PathVariable Long id) {
        return reviewService.getDetail(id);
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody ReviewReplyRequest request) {
        return reviewService.reply(id, request);
    }

    @PostMapping("/{id}/reply-with-template")
    public Result<Void> replyWithTemplate(@PathVariable Long id, @RequestBody ReviewReplyTemplateRequest request) {
        return reviewService.replyWithTemplate(id, request);
    }

    @GetMapping("/statistics")
    public Result<ReviewStatisticsVO> statistics(@RequestParam(required = false) Long shopId) {
        ReviewStatisticsDTO dto = new ReviewStatisticsDTO();
        dto.setShopId(shopId);
        return reviewService.getStatistics(dto);
    }

    @GetMapping("/for-ticket")
    public Result<PageResult<ReviewForTicketVO>> getReviewsForTicket(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ticketService.getReviewsForTicket(shopId, starRating, page, pageSize);
    }
}
