package com.movie.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.OrderDTO;
import com.movie.dto.PayDTO;
import com.movie.entity.CinemaHall;
import com.movie.entity.Info;
import com.movie.entity.Order;
import com.movie.entity.Schedule;
import com.movie.entity.User;
import com.movie.service.ICinemaHallService;
import com.movie.service.IMovieService;
import com.movie.service.IOrderService;
import com.movie.service.IScheduleService;
import com.movie.utils.UserHolder;
import com.movie.vo.SeatInfoVO;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private IOrderService orderService;
    @Autowired
    private IScheduleService scheduleService;
    @Autowired
    private IMovieService movieService;
    @Autowired
    private ICinemaHallService hallService;

    // 获取座位图
    @GetMapping("/seats/{scheduleId}")
    public Result<SeatInfoVO> getSeats(@PathVariable Long scheduleId) {
        try {
            return Result.success(orderService.getSeatInfo(scheduleId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 创建订单（锁座）
    @PostMapping("/create")
    @SuppressWarnings("CallToPrintStackTrace")
    public Result<String> create(@RequestBody OrderDTO dto) {
        Long userId = UserHolder.getUser().getId();
        try {
            String orderNo = orderService.createOrder(dto, userId);
            return Result.success(orderNo); // 返回订单号
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    // 支付订单 (需要登录)
    @PostMapping("/pay") // 改成 POST Body 传参更安全
    public Result<String> pay(@RequestBody PayDTO dto) {
        User user = UserHolder.getUser();
        try {
            orderService.payOrder(dto, user.getId());
            return Result.success("支付成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 退款订单 (需要登录)
    @PostMapping("/refund/{orderNo}")
    public Result<String> refund(@PathVariable String orderNo) {
        User user = UserHolder.getUser();
        if (user == null) return Result.error("请先登录");
        try {
            orderService.refundOrder(orderNo, user.getId());
            return Result.success("退款成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 手动取消订单 (需要登录)
    @PostMapping("/cancel/{orderNo}")
    public Result<String> cancel(@PathVariable String orderNo) {
        User user = UserHolder.getUser();
        if (user == null) return Result.error("请先登录");
        try {
            orderService.cancelOrder(orderNo, user.getId());
            return Result.success("订单已取消");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 我的订单列表
    @GetMapping("/my")
    public Result<Page<Order>> myOrders(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) Integer status) {
        Long userId = UserHolder.getUser().getId();
        Page<Order> pageParam = new Page<>(page, size);
        
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (status != null) {
            query.eq("status", status); 
        }
        query.orderByDesc("create_time");
        
        Page<Order> result = orderService.page(pageParam, query);

        // --- 【核心修改】遍历订单，填充电影标题 ---
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            for (Order order : result.getRecords()) {
                // 1. 查排片
                Schedule schedule = scheduleService.getById(order.getScheduleId());
                if (schedule != null) {
                    // 2. 查电影
                    Info movie = movieService.getById(schedule.getMovieId());
                    if (movie != null) {
                        order.setMovieTitle(movie.getTitle()); // 填充到非数据库字段
                    }
                }
            }
        }
        // ----------------------------------------
        
        return Result.success(result);
    }

    @SysLog("管理员查询订单列表")
    @GetMapping("/admin/list")
    public Result<Page<Order>> adminList(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String orderNo) {
        Page<Order> pageParam = new Page<>(page, size);
        QueryWrapper<Order> query = new QueryWrapper<>();
        
        // 如果输入了订单号，就精确查询
        if (orderNo != null && !orderNo.isEmpty()) {
            query.eq("order_no", orderNo);
        }
        
        query.orderByDesc("create_time"); // 最新订单在前
        return Result.success(orderService.page(pageParam, query));
    }

    @GetMapping("/detail/{orderNo}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable String orderNo) {
        // 1. 查订单基础信息
        Order order = orderService.getOne(new QueryWrapper<Order>().eq("order_no", orderNo));
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 2. 查排片
        Schedule schedule = scheduleService.getById(order.getScheduleId());
        
        // 3. 查电影、影厅
        // 注意判空，防止脏数据导致空指针
        String movieTitle = "未知电影";
        String hallName = "未知影厅";
        String startTime = "";
        
        if (schedule != null) {
            Info movie = movieService.getById(schedule.getMovieId());
            if (movie != null) movieTitle = movie.getTitle();
            
            CinemaHall hall = hallService.getById(schedule.getHallId());
            if (hall != null) hallName = hall.getName();
            
            startTime = schedule.getStartTime().toString();
        }

        // 4. 组装结果 (Map 或 VO)
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("totalPrice", order.getTotalPrice());
        result.put("seatInfo", order.getSeatInfo());
        result.put("status", order.getStatus());
        // 补充信息
        result.put("movieTitle", movieTitle);
        result.put("hallName", hallName);
        result.put("startTime", startTime);

        return Result.success(result);
    }
}