package com.movie.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SeatInfoVO {
    private Map<String, Object> hallConfig; // 改成 Object，因为里面可能有 List
    private List<String> soldSeats;
    private List<String> brokenSeats; // 新增：坏座列表
    private String movieTitle;    // 电影名
    private String cinemaName;    // 影院名
    private String hallName;      // 影厅名
    private BigDecimal price;     // 票价
    private LocalDateTime startTime;

    // --- 手动生成 Getter/Setter ---
    public Map<String, Object> getHallConfig() { return hallConfig; }
    public void setHallConfig(Map<String, Object> hallConfig) { this.hallConfig = hallConfig; }
    public List<String> getSoldSeats() { return soldSeats; }
    public void setSoldSeats(List<String> soldSeats) { this.soldSeats = soldSeats; }
    
    public List<String> getBrokenSeats() { return brokenSeats; }
    public void setBrokenSeats(List<String> brokenSeats) { this.brokenSeats = brokenSeats; }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}