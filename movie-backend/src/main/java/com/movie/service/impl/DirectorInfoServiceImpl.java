package com.movie.service.impl;

import com.movie.entity.DirectorInfo;
import com.movie.mapper.DirectorInfoMapper;
import com.movie.service.IDirectorInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 导演信息表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class DirectorInfoServiceImpl extends ServiceImpl<DirectorInfoMapper, DirectorInfo> implements IDirectorInfoService {

}
