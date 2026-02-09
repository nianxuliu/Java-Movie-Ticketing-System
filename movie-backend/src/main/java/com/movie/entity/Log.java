package com.movie.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <p>
 * 后台操作日志表
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@TableName("sys_log")
@ApiModel(value = "Log对象", description = "后台操作日志表")
public class Log implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("操作人")
    @TableField("username")
    private String username;

    @ApiModelProperty("操作模块(如: 电影管理)")
    @TableField("module")
    private String module;

    @ApiModelProperty("动作(如: 删除电影)")
    @TableField("action")
    private String action;

    @ApiModelProperty("请求参数(JSON)")
    @TableField("params")
    private String params;

    @ApiModelProperty("操作人IP")
    @TableField("ip")
    private String ip;

    @TableField("create_time")
    private LocalDateTime createTime;
    
    @TableField("method")
    private String method;
    
    @TableField("time")
    private Long time; // 执行时长

    // 手动生成 Getter/Setter
    public Long getTime() { return time; }
    public void setTime(Long time) { this.time = time; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
