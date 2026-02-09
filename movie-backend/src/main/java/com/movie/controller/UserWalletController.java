package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movie.common.Result;
import com.movie.dto.ChangePayPwdDTO;
import com.movie.dto.RechargeDTO;
import com.movie.dto.WalletPasswordDTO;
import com.movie.entity.UserWallet;
import com.movie.service.IUserWalletService;
import com.movie.utils.UserHolder;

/**
 * <p>
 * 用户钱包表 前端控制器
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/userWallet")
public class UserWalletController {
    @Autowired
    private IUserWalletService userWalletService;

    // 设置支付密码 (需要登录)
    @PostMapping("/set-password")
    public Result<String> setPassword(@RequestBody WalletPasswordDTO dto) {
        Long userId = UserHolder.getUser().getId();
        try {
            userWalletService.setPayPassword(userId, dto.getPayPassword());
            return Result.success("支付密码设置成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 修改支付密码
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody ChangePayPwdDTO dto) {
        Long userId = UserHolder.getUser().getId();
        try {
            userWalletService.changePayPassword(userId, dto);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 充值接口 (需要登录)
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestBody RechargeDTO dto) {
        Long userId = UserHolder.getUser().getId();
        try {
            userWalletService.recharge(userId, dto.getAmount());
            return Result.success("充值成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    // 查询钱包详情 (余额) - 这个也很重要，前端要展示
    @GetMapping("/info")
    public Result<UserWallet> getWalletInfo() {
        Long userId = UserHolder.getUser().getId();
        UserWallet wallet = userWalletService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserWallet>().eq("user_id", userId)
        );
        if(wallet != null) {
            wallet.setPayPassword(null); // 脱敏，不返回支付密码
        }
        return Result.success(wallet);
    }
}
