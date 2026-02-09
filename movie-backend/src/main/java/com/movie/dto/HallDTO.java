package com.movie.dto;

public class HallDTO {
    private Long cinemaId;
    private String name;
    private String seatConfig; // 例如 "10x8"

    // --- 手动生成 Getters/Setters ---
    public Long getCinemaId() { return cinemaId; }
    public void setCinemaId(Long cinemaId) { this.cinemaId = cinemaId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSeatConfig() { return seatConfig; }
    public void setSeatConfig(String seatConfig) { this.seatConfig = seatConfig; }
}