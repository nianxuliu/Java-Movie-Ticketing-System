package com.movie.dto;

public class ChangePayPwdDTO {
    private String oldPassword;
    private String newPassword;

    // Getter/Setter 手动生成
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}