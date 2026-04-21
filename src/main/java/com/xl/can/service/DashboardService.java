package com.xl.can.service;

import com.xl.can.common.Result;
import com.xl.can.vo.DashboardVO;

public interface DashboardService {

    Result<DashboardVO> getStatistics();
}
