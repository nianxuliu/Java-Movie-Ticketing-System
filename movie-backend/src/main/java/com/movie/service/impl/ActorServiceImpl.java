package com.movie.service.impl;

import com.movie.entity.Actor;
import com.movie.mapper.ActorMapper;
import com.movie.service.IActorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 电影演员关联表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class ActorServiceImpl extends ServiceImpl<ActorMapper, Actor> implements IActorService {

}
