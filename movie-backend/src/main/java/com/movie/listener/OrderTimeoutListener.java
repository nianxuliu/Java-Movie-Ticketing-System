package com.movie.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.config.RabbitMQConfig;
import com.movie.entity.Order;
import com.movie.service.IOrderService;

@Component
public class OrderTimeoutListener {
    @Autowired
    private IOrderService orderService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 监听死信队列
    @RabbitListener(queues = RabbitMQConfig.ORDER_RELEASE_QUEUE)
    public void listenOrderTimeout(String orderNo) {
        if (orderNo == null) return;
        
        // 1. 根据订单号查询订单
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("order_no", orderNo);
        Order order = orderService.getOne(query);

        if (order == null) return;

        // 2. 检查订单状态是否仍为“待支付”
        if (order.getStatus() == 0) {
            // 3. 状态仍为 0，说明超时未支付，关闭订单
            order.setStatus(2); // 2-已取消
            orderService.updateById(order);

            // 4. 释放 Redis 中的座位锁
            String lockKeyPrefix = "lock:seat:" + order.getScheduleId() + ":";
            List<String> seats = List.of(order.getSeatInfo().split(","));
            List<String> lockKeys = seats.stream().map(seat -> lockKeyPrefix + seat).collect(Collectors.toList());
            redisTemplate.delete(lockKeys);
        }
    }
}