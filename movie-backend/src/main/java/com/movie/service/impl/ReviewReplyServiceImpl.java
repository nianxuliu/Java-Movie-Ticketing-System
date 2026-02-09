package com.movie.service.impl;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.config.RabbitMQConfig;
import com.movie.dto.LikeMsgDTO;
import com.movie.entity.ReviewReply;
import com.movie.mapper.ReviewReplyMapper;
import com.movie.service.IReviewReplyService;
import com.movie.vo.ReplyVO;

/**
 * <p>
 * 评论回复表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class ReviewReplyServiceImpl extends ServiceImpl<ReviewReplyMapper, ReviewReply> implements IReviewReplyService {
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private RabbitTemplate rabbitTemplate;

    // 点赞回复
    public void likeReply(Long replyId, Long userId) {
        String key = "reply:like:" + replyId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());
        boolean isLikeAction;

        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.opsForSet().remove(key, userId.toString());
            isLikeAction = false;
        } else {
            redisTemplate.opsForSet().add(key, userId.toString());
            isLikeAction = true;
        }

        // 发送完整 DTO
        LikeMsgDTO msg = new LikeMsgDTO(userId, replyId, 2, isLikeAction); // type=2
        rabbitTemplate.convertAndSend(RabbitMQConfig.LIKE_COUNT_QUEUE, msg);
    }
    
    @Override
    public void loadLikeState(List<ReplyVO> list, Long userId) {
        // 删除这行：if (userId == null) return; 
        // 即使没登录，也要显示有多少人点赞
        
        if (list == null || list.isEmpty()) return;

        for (ReplyVO vo : list) {
            String key = "reply:like:" + vo.getId();
            
            // 1. 先查总数 (Redis 数据是最准的)
            Long count = redisTemplate.opsForSet().size(key);
            vo.setLikeCount(count != null ? count.intValue() : 0);

            // 2. 如果用户登录了，再查该用户是否点赞
            if (userId != null) {
                // 必须转 String，防止 Long 和 String 比较导致永远 false
                Boolean isLiked = redisTemplate.opsForSet().isMember(key, userId.toString());
                vo.setIsLiked(Boolean.TRUE.equals(isLiked));
            } else {
                // 没登录肯定没点赞
                vo.setIsLiked(false);
            }
        }
    }

    @Override
    public Page<ReplyVO> getReplyPage(Long reviewId, int pageNum, int pageSize) {
        Page<ReplyVO> page = new Page<>(pageNum, pageSize);
        // 直接调用 Mapper 的分页方法
        return baseMapper.findRepliesByReviewId(page, reviewId);
    }
}
