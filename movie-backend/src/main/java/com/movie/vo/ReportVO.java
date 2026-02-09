package com.movie.vo;

import java.math.BigDecimal;

public class ReportVO {
    private BigDecimal totalBoxOffice; // 总票房
    private BigDecimal todayBoxOffice; // 今日票房
    private Long totalUsers;           // 总用户数
    private Long totalOrders;          // 总订单数

    // --- 手动生成 Getter/Setter ---
    public BigDecimal getTotalBoxOffice() { return totalBoxOffice; }
    public void setTotalBoxOffice(BigDecimal totalBoxOffice) { this.totalBoxOffice = totalBoxOffice; }
    public BigDecimal getTodayBoxOffice() { return todayBoxOffice; }
    public void setTodayBoxOffice(BigDecimal todayBoxOffice) { this.todayBoxOffice = todayBoxOffice; }
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
}