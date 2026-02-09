package com.movie.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.dto.ChangePayPwdDTO;
import com.movie.entity.UserWallet;
import com.movie.entity.WalletLog;
import com.movie.mapper.UserWalletMapper;
import com.movie.service.IUserWalletService;
import com.movie.service.IWalletLogService;

import cn.hutool.crypto.digest.BCrypt;

/**
 * <p>
 * 用户钱包表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements IUserWalletService {
    
    @Autowired
    private IWalletLogService walletLogService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPayPassword(Long userId, String password) {
        // 1. 查询钱包
        QueryWrapper<UserWallet> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        UserWallet wallet = this.getOne(query);

        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 2. 校验密码强度 (企业级做法：必须是6位数字)
        if (!password.matches("^\\d{6}$")) {
            throw new RuntimeException("支付密码必须是6位数字");
        }

        // 3. 加密并保存
        String encodedPwd = BCrypt.hashpw(password);
        wallet.setPayPassword(encodedPwd);
        
        this.updateById(wallet);
    }

    @Override
    public void changePayPassword(Long userId, ChangePayPwdDTO dto) {
        UserWallet wallet = this.getOne(new QueryWrapper<UserWallet>().eq("user_id", userId));
        if (wallet == null) throw new RuntimeException("钱包不存在");
        
        // 1. 校验旧密码
        if (wallet.getPayPassword() != null) {
            if (!BCrypt.checkpw(dto.getOldPassword(), wallet.getPayPassword())) {
                throw new RuntimeException("旧支付密码错误");
            }
        }
        
        // 2. 校验新密码格式
        if (!dto.getNewPassword().matches("^\\d{6}$")) {
            throw new RuntimeException("新密码必须是6位数字");
        }

        // 3. 更新
        wallet.setPayPassword(BCrypt.hashpw(dto.getNewPassword()));
        this.updateById(wallet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        // 1. 基础校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            throw new RuntimeException("单次充值不能超过 10万"); // 风控限制
        }

        // 2. 查询钱包
        UserWallet wallet = this.getOne(new QueryWrapper<UserWallet>().eq("user_id", userId));
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 3. 增加余额
        wallet.setBalance(wallet.getBalance().add(amount));
        boolean updateSuccess = this.updateById(wallet);
        if (!updateSuccess) {
            throw new RuntimeException("充值失败");
        }

        // 4. 记录流水 (Type = 1: 充值)
        WalletLog walletLog = new WalletLog();
        walletLog.setUserId(userId);
        walletLog.setAmount(amount); // 正数
        walletLog.setType((byte) 1); // 1-充值
        
        // 生成一个充值流水号，避免和其他订单号混淆
        walletLog.setOrderNo("RECH" + System.currentTimeMillis()); 
        walletLog.setRemark("余额充值");
        
        walletLogService.save(walletLog);
    }
}
