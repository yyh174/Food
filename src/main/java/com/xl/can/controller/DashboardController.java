package com.xl.can.controller;

import com.xl.can.common.Result;
import com.xl.can.service.DashboardService;
import com.xl.can.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/statistics")
    public Result<DashboardVO> getStatistics() {
        return dashboardService.getStatistics();
    }
}
