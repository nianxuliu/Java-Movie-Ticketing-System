package com.movie.dto;

import java.math.BigDecimal;

public class ScheduleDTO {
    private Long cinemaId;
    private Long hallId;
    private Long movieId;
    private String startTime;
    private BigDecimal price;
    
    // --- 手动生成 Getters/Setters ---
    public Long getCinemaId() { return cinemaId; }
    public void setCinemaId(Long cinemaId) { this.cinemaId = cinemaId; }
    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}