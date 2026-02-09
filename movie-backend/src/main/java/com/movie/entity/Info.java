package com.movie.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 电影信息表
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@TableName("movie_info")
@ApiModel(value = "Info对象", description = "电影信息表")
public class Info implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("电影ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("电影名称")
    @TableField("title")
    private String title;

    @ApiModelProperty("原名(英文名)")
    @TableField("original_title")
    private String originalTitle;

    @ApiModelProperty("上映日期")
    @TableField("release_date")
    private LocalDate releaseDate;

    @ApiModelProperty("时长(分钟)")
    @TableField("duration")
    private Integer duration;

    @ApiModelProperty("类型(如: 剧情,动作)")
    @TableField("genre")
    private String genre;

    @ApiModelProperty("语言")
    @TableField("language")
    private String language;

    @ApiModelProperty("制片国家/地区")
    @TableField("country")
    private String country;

    @ApiModelProperty("剧情简介")
    @TableField("synopsis")
    private String synopsis;

    @ApiModelProperty("海报图片地址(MinIO)")
    @TableField("poster_url")
    private String posterUrl;

    @ApiModelProperty("预告片视频地址")
    @TableField("trailer_url")
    private String trailerUrl;

    @ApiModelProperty("综合评分(如 8.5)")
    @TableField("rating")
    private BigDecimal rating;

    @ApiModelProperty("评论总数")
    @TableField("review_count")
    private Integer reviewCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
