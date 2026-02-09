package com.movie.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.entity.User;
import com.movie.mapper.OrderMapper;
import com.movie.mapper.UserMapper;
import com.movie.vo.ReportVO;

@Service
public class ReportService {

    @Autowired
    private OrderMapper orderMapper; // 查票房
    @Autowired
    private UserMapper userMapper;   // 查人数

    public ReportVO getStatistics() {
        ReportVO vo = new ReportVO();

        // 1. 总票房
        BigDecimal totalBoxOffice = orderMapper.sumTotalBoxOffice();
        vo.setTotalBoxOffice(totalBoxOffice);

        // 2. 今日票房
        BigDecimal todayBoxOffice = orderMapper.sumTodayBoxOffice();
        vo.setTodayBoxOffice(todayBoxOffice);

        // 3. 总用户数
        @SuppressWarnings("Convert2Diamond")
        Long userCount = userMapper.selectCount(new QueryWrapper<User>());
        vo.setTotalUsers(userCount);

        // 4. 总订单数 (包括未支付的，看老板需求，通常统计成交单，这里我们统计所有)
        // 这里只统计已支付的订单数作为“有效订单”
        Long orderCount = orderMapper.selectCount(new QueryWrapper<com.movie.entity.Order>().eq("status", 1));
        vo.setTotalOrders(orderCount);

        return vo;
    }
}