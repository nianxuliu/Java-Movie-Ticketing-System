package com.movie.service.impl;

import com.movie.entity.WalletLog;
import com.movie.mapper.WalletLogMapper;
import com.movie.service.IWalletLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 交易流水表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class WalletLogServiceImpl extends ServiceImpl<WalletLogMapper, WalletLog> implements IWalletLogService {

}
