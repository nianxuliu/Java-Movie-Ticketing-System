package com.movie.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.entity.Schedule;
import com.movie.mapper.ScheduleMapper;
import com.movie.service.IScheduleService;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements IScheduleService {
    @Override
    public boolean isTimeConflict(Long hallId, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Schedule> query = new QueryWrapper<>();
        query.eq("hall_id", hallId)
            .lt("start_time", endTime) // 已有排片的开始时间 < 新排片的结束时间
            .gt("end_time", startTime);  // 已有排片的结束时间 > 新排片的开始时间

        return this.count(query) > 0;
    }
}