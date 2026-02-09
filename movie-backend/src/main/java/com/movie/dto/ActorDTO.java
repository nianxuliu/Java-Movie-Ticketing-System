package com.movie.dto;

import java.time.LocalDate;

public class ActorDTO {
    private String name;
    private String enName;
    private Integer gender; // 0女 1男
    private LocalDate birthDate; // 格式: "1980-01-01"
    private String nationality;
    private String avatarUrl;

    // --- 右键 -> Source Action -> Generate Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEnName() { return enName; }
    public void setEnName(String enName) { this.enName = enName; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}