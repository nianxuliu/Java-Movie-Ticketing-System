package com.movie.service.impl;

import com.movie.entity.Log;
import com.movie.mapper.LogMapper;
import com.movie.service.ILogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台操作日志表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements ILogService {

}
