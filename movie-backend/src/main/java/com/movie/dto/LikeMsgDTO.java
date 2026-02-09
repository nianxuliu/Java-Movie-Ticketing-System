package com.movie.dto;

import java.io.Serializable;

public class LikeMsgDTO implements Serializable {
    private Long userId;
    private Long targetId; // 评论ID 或 回复ID
    private Integer type;  // 1-影评 2-回复
    private Boolean isLike; // true-点赞 false-取消

    // 构造函数
    public LikeMsgDTO() {}
    public LikeMsgDTO(Long userId, Long targetId, Integer type, Boolean isLike) {
        this.userId = userId;
        this.targetId = targetId;
        this.type = type;
        this.isLike = isLike;
    }

    // Getter/Setter 手动生成
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Boolean getIsLike() { return isLike; }
    public void setIsLike(Boolean isLike) { this.isLike = isLike; }
}