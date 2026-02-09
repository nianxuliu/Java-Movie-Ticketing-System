package com.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.dto.OrderDTO;
import com.movie.dto.PayDTO;
import com.movie.entity.Order;
import com.movie.vo.SeatInfoVO;

/**
 * <p>
 * 购票订单表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IOrderService extends IService<Order> {
    public String createOrder(OrderDTO dto, Long userId);
    public SeatInfoVO getSeatInfo(Long scheduleId) throws Exception;
    void payOrder(PayDTO dto, Long userId);
    void refundOrder(String orderNo, Long userId);
    void cancelOrder(String orderNo, Long userId);
}
