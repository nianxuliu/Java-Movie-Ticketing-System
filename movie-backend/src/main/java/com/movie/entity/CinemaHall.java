package com.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 影厅信息表
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@TableName("cinema_hall")
@ApiModel(value = "CinemaHall对象", description = "影厅信息表")
public class CinemaHall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("所属影院")
    @TableField("cinema_id")
    private Long cinemaId;

    @ApiModelProperty("影厅名(如: 1号IMAX厅)")
    @TableField("name")
    private String name;

    @ApiModelProperty("座位布局JSON(行数,列数,坏座位置)")
    @TableField("seat_config")
    private String seatConfig;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Long cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeatConfig() {
        return seatConfig;
    }

    public void setSeatConfig(String seatConfig) {
        this.seatConfig = seatConfig;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
