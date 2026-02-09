package com.movie.dto;

public class CinemaDTO {
    private String name;
    private String address;
    private String city;
    
    // --- 手动生成 Getters/Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}