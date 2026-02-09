package com.movie.service;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.dto.ChangePayPwdDTO;
import com.movie.entity.UserWallet;

/**
 * <p>
 * 用户钱包表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IUserWalletService extends IService<UserWallet> {
    void setPayPassword(Long userId, String password);
    void changePayPassword(Long userId, ChangePayPwdDTO dto);
    void recharge(Long userId, BigDecimal amount);
}
