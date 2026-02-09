package com.movie.service.impl;

import com.movie.entity.ActorInfo;
import com.movie.mapper.ActorInfoMapper;
import com.movie.service.IActorInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 演员信息表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class ActorInfoServiceImpl extends ServiceImpl<ActorInfoMapper, ActorInfo> implements IActorInfoService {

}
