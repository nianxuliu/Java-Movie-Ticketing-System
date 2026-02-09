package com.movie.service.impl;

import com.movie.entity.UserLikeRecord;
import com.movie.mapper.UserLikeRecordMapper;
import com.movie.service.IUserLikeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户点赞记录 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class UserLikeRecordServiceImpl extends ServiceImpl<UserLikeRecordMapper, UserLikeRecord> implements IUserLikeRecordService {

}
