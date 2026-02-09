package com.movie.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 购票订单表
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@TableName("movie_order")
@ApiModel(value = "Order对象", description = "购票订单表")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单号(UUID)")
    @TableId("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @ApiModelProperty("场次ID")
    @TableField("schedule_id")
    private Long scheduleId;

    @ApiModelProperty("座位号(如 \"5排6座,5排7座\")")
    @TableField("seat_info")
    private String seatInfo;

    @ApiModelProperty("总金额")
    @TableField("total_price")
    private BigDecimal totalPrice;

    @ApiModelProperty("0:待支付 1:已支付 2:已取消 3:已退款")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("支付时间")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String movieTitle;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getSeatInfo() {
        return seatInfo;
    }

    public void setSeatInfo(String seatInfo) {
        this.seatInfo = seatInfo;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }
}
