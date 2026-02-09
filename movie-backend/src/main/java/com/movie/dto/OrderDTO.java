package com.movie.dto;

import java.util.List;

public class OrderDTO {
    private Long scheduleId;
    private List<String> seats; // 座位列表, 例如 ["3-4", "3-5"] (3排4座, 3排5座)

    // --- 手动生成 Getters/Setters ---
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }
}