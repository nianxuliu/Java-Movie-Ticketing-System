package com.movie.vo;

import java.io.Serializable;

public class UserLoginVO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String token; // 最重要的字段
    private Boolean isAdmin;
    private String role; // 新增: "admin" 或 "user"

    // --- 手动生成 Getter/Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean admin) { isAdmin = admin; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}