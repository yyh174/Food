package com.xl.can.service;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.dto.*;
import com.xl.can.vo.*;

import java.util.List;

public interface TicketService {
    Result<PageResult<TicketListVO>> list(TicketListDTO dto);
    Result<TicketDetailVO> getDetail(Long id);
    Result<TicketListVO> create(TicketCreateDTO dto);
    Result<Void> assign(Long id, TicketAssignDTO dto);
    Result<Void> submit(Long id, TicketActionDTO dto);
    Result<Void> verify(Long id, TicketActionDTO dto);
    Result<Void> archive(Long id, TicketActionDTO dto);
    Result<Void> reject(Long id, TicketRejectDTO dto);
    Result<TicketStatisticsVO> statistics(TicketStatisticsDTO dto);
    Result<PageResult<ReviewForTicketVO>> getReviewsForTicket(Long shopId, Integer starRating, Integer page, Integer pageSize);
}
