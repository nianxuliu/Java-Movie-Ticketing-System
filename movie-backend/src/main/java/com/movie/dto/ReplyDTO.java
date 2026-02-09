package com.movie.dto;

public class ReplyDTO {
    private Long reviewId; // 回复哪条主评论
    private Long targetUserId; // 回复谁
    private String content;

    // 手动生成 Getter/Setter
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}