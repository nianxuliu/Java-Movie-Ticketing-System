package com.movie.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.entity.Schedule;

/**
 * <p>
 * 电影排片表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IScheduleService extends IService<Schedule> {
    boolean isTimeConflict(Long hallId, LocalDateTime startTime, LocalDateTime endTime);
}
