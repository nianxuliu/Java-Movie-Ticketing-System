package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.common.Result;
import com.movie.entity.Log;
import com.movie.service.ILogService;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private ILogService logService;

    // 分页查询日志 (支持按用户名或操作内容搜索)
    @GetMapping("/list")
    public Result<Page<Log>> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String keyword) {
        Page<Log> pageParam = new Page<>(page, size);
        QueryWrapper<Log> query = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            query.and(wrapper -> wrapper
                .like("username", keyword)
                .or()
                .like("action", keyword)
            );
        }
        query.orderByDesc("create_time");
        
        return Result.success(logService.page(pageParam, query));
    }
}