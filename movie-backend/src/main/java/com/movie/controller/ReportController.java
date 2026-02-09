package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.service.ReportService;
import com.movie.vo.ReportVO;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @SysLog("获取统计数据")
    @GetMapping("/index")
    public Result<ReportVO> getIndexStats() {
        return Result.success(reportService.getStatistics());
    }
}