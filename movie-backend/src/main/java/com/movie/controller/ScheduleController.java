package com.movie.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.ScheduleDTO;
import com.movie.entity.Info;
import com.movie.entity.Order;
import com.movie.entity.Schedule;
import com.movie.service.ICinemaHallService;
import com.movie.service.IMovieService;
import com.movie.service.IOrderService;
import com.movie.service.IScheduleService;
import com.movie.websocket.WebSocketServer;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private IScheduleService scheduleService;
    @Autowired
    private IMovieService infoService;
    @Autowired
    private ICinemaHallService hallService;
    @Autowired
    private IOrderService orderService; // 注入订单服务用来检查

    @SysLog("添加排片")
    @PostMapping("/add")
    public Result<String> add(@RequestBody ScheduleDTO dto) {
        // 校验1：电影是否存在
        Info movie = infoService.getById(dto.getMovieId());
        if (movie == null) {
            return Result.error("电影不存在！");
        }

        // 校验2：影厅是否存在
        if (hallService.getById(dto.getHallId()) == null) {
            return Result.error("影厅不存在！");
        }

        // 计算结束时间
        LocalDateTime startTime = LocalDateTime.parse(dto.getStartTime()); 
        Integer duration = movie.getDuration();
        LocalDateTime endTime = startTime.plusMinutes(duration + 15);

        // --- 核心校验3：时间冲突检测 ---
        if (scheduleService.isTimeConflict(dto.getHallId(), startTime, endTime)) {
            return Result.error("时间冲突！");
        }

        // 所有校验通过，保存排片
        Schedule schedule = new Schedule();
        schedule.setCinemaId(dto.getCinemaId());
        schedule.setHallId(dto.getHallId());
        schedule.setMovieId(dto.getMovieId());
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime); // 保存计算好的结束时间
        schedule.setPrice(dto.getPrice());

        scheduleService.save(schedule);
        String msg = "新片上映提醒：电影《" + movie.getTitle() + "》新增了场次，快来抢票吧！";
        WebSocketServer.sendAll(msg); 
        return Result.success("排片添加成功");
    }

    @SysLog("删除排片")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 1. 检查是否有相关订单 (包含待支付、已支付、已取消、已退款的所有记录)
        // 只要产生过数据，为了财务审计，通常建议保留，或者只允许删除无订单的排片
        long count = orderService.count(new QueryWrapper<Order>().eq("schedule_id", id));
        if (count > 0) {
            return Result.error("该场次已有订单数据，禁止删除！建议下架或锁定。");
        }

        scheduleService.removeById(id);
        return Result.success("删除成功");
    }

    // 排片列表 (根据电影ID查询，只显示未开场的)
    @GetMapping("/list/{movieId}")
    public Result<List<Schedule>> listByMovie(@PathVariable Long movieId) {
        QueryWrapper<Schedule> query = new QueryWrapper<>();
        query.eq("movie_id", movieId)
             .gt("start_time", LocalDateTime.now()) // 核心：start_time > 现在
             .orderByAsc("start_time"); // 按时间先后排序
        
        return Result.success(scheduleService.list(query));
    }

    @GetMapping("/admin/list/{movieId}")
    public Result<List<Schedule>> adminList(@PathVariable Long movieId) {
        QueryWrapper<Schedule> query = new QueryWrapper<>();
        query.eq("movie_id", movieId)
             .orderByAsc("start_time"); // 管理员需要看所有的，包括过期的
        return Result.success(scheduleService.list(query));
    }
}