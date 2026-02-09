package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.common.Result;
import com.movie.entity.WalletLog;
import com.movie.service.IWalletLogService;
import com.movie.utils.UserHolder;

@RestController
@RequestMapping("/wallet-log")
public class WalletLogController {

    @Autowired
    private IWalletLogService walletLogService;

    // 分页查询我的流水
    @GetMapping("/my")
    public Result<Page<WalletLog>> myLogs(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        Long userId = UserHolder.getUser().getId();
        Page<WalletLog> pageParam = new Page<>(page, size);
        QueryWrapper<WalletLog> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("create_time");
        
        return Result.success(walletLogService.page(pageParam, query));
    }
}