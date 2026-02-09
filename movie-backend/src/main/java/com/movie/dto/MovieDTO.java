package com.movie.dto;

import java.time.LocalDate;
import java.util.List;

public class MovieDTO {
    private Long id;
    // 对应 Info 实体类的字段
    private String title;
    private String originalTitle;
    private LocalDate releaseDate; 
    private Integer duration;
    private String genre;
    private String language;
    private String country;
    private String synopsis;
    private String posterUrl;
    
    // --- 关键：接收关联的 ID 列表 ---
    private List<Long> actorIds;   // 演员ID列表 (例如: [1, 2])
    private List<Long> directorIds;// 导演ID列表 (例如: [3])

    // --- 请务必手动生成 Getter / Setter ---
    // 右键 -> Source Action -> Generate Getters and Setters -> Select All
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    
    public List<Long> getActorIds() { return actorIds; }
    public void setActorIds(List<Long> actorIds) { this.actorIds = actorIds; }
    public List<Long> getDirectorIds() { return directorIds; }
    public void setDirectorIds(List<Long> directorIds) { this.directorIds = directorIds; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}