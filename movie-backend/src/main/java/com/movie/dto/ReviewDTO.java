package com.movie.dto;

import java.math.BigDecimal;

public class ReviewDTO {
    private Long movieId;
    private BigDecimal score; // 评分，例如 8.5
    private String content; // 评论内容

    // --- 手动生成 Getter/Setter ---
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}