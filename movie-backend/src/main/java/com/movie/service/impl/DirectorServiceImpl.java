package com.movie.service.impl;

import com.movie.entity.Director;
import com.movie.mapper.DirectorMapper;
import com.movie.service.IDirectorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 电影导演关联表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class DirectorServiceImpl extends ServiceImpl<DirectorMapper, Director> implements IDirectorService {

}
