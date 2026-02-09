package com.movie.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.movie.mapper.OrderMapper;

@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每分钟执行一次 (Cron表达式: 秒 分 时 日 月 周)
     * 检查是否有电影已经放完了，如果有，把订单改成已观影
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processWatchedOrders() {
        // System.out.println("执行定时任务：更新已观影状态 - " + LocalDateTime.now());
        orderMapper.updateWatchedStatus();
    }
}