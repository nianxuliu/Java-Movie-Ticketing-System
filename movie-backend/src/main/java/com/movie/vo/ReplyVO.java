package com.movie.vo;



import com.movie.entity.ReviewReply;

public class ReplyVO extends ReviewReply {
    private String nickname;   // 回复人昵称
    private String avatarUrl;  // 回复人头像
    private String targetNickname; // 被回复人昵称
    private Boolean isLiked;  // 是否已点赞

    // 手动生成 Getter/Setter
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getTargetNickname() { return targetNickname; }
    public void setTargetNickname(String targetNickname) { this.targetNickname = targetNickname; }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

}