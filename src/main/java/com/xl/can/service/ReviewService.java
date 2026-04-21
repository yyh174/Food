package com.xl.can.service;

import com.xl.can.common.PageResult;
import com.xl.can.common.Result;
import com.xl.can.dto.ReviewListDTO;
import com.xl.can.dto.ReviewReplyRequest;
import com.xl.can.dto.ReviewReplyTemplateRequest;
import com.xl.can.dto.ReviewStatisticsDTO;
import com.xl.can.vo.ReviewStatisticsVO;
import com.xl.can.vo.ReviewVO;

public interface ReviewService {

    Result<PageResult<ReviewVO>> pageList(ReviewListDTO dto);

    Result<ReviewVO> getDetail(Long id);

    Result<Void> reply(Long id, ReviewReplyRequest request);

    Result<Void> replyWithTemplate(Long id, ReviewReplyTemplateRequest request);

    Result<ReviewStatisticsVO> getStatistics(ReviewStatisticsDTO dto);
}
