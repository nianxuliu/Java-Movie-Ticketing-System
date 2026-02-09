package com.movie.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movie.entity.Order;

/**
 * <p>
 * 购票订单表 Mapper 接口
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface OrderMapper extends BaseMapper<Order> {
    // 统计总票房 (只算已支付 status=1 的)
    @Select("SELECT IFNULL(SUM(total_price), 0) FROM movie_order WHERE status = 1")
    BigDecimal sumTotalBoxOffice();

    // 统计今日票房 (只算已支付 + 支付时间是今天)
    @Select("SELECT IFNULL(SUM(total_price), 0) FROM movie_order WHERE status = 1 AND DATE(pay_time) = CURDATE()")
    BigDecimal sumTodayBoxOffice();

    // 定时任务专用：把所有 "已支付(1)" 且 "结束时间小于当前时间" 的订单，改为 "已观影(4)"
    // 这里用了连表更新 (UPDATE JOIN)
    @Update("UPDATE movie_order o " +
            "INNER JOIN movie_schedule s ON o.schedule_id = s.id " +
            "SET o.status = 4 " +
            "WHERE o.status = 1 AND s.end_time < NOW()")
    void updateWatchedStatus();
}
