package com.movie.vo;

import java.util.List;

import com.movie.entity.Review; // 引入实体类

public class ReviewVO extends Review { // 直接继承，获得所有字段
    private String movieTitle; // 新增：电影标题
    private String nickname;
    private String avatarUrl;
    private Boolean isLiked; // 新增：当前用户是否已点赞
    private List<ReplyVO> replyList;

    // --- 手动生成 Getter/Setter ---
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean liked) { isLiked = liked; }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public List<ReplyVO> getReplyList() {
        return replyList;
    }

    public void setReplyList(List<ReplyVO> replyList) {
        this.replyList = replyList;
    }
}