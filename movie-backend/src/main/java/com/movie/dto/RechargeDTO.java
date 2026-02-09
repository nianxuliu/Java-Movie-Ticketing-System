package com.movie.dto;

import java.math.BigDecimal;

public class RechargeDTO {
    private BigDecimal amount; // 充值金额

    // 手动生成 Getter/Setter
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}