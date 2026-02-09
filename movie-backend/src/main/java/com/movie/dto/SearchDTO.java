package com.movie.dto;

public class SearchDTO {
    private String keyword; // 搜索关键词
    private int page = 1;   // 页码，默认第1页
    private int size = 10;  // 每页大小，默认10条

    // --- 手动生成 Getters and Setters ---
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}