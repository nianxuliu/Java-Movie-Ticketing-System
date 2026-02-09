package com.movie.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.config.RabbitMQConfig;
import com.movie.dto.LikeMsgDTO;
import com.movie.entity.Review;
import com.movie.entity.ReviewReply;
import com.movie.entity.UserLikeRecord;
import com.movie.service.IReviewReplyService;
import com.movie.service.IReviewService;
import com.movie.service.IUserLikeRecordService;

@Component
public class LikeCountListener {

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private IReviewService reviewService;
    @Autowired private IReviewReplyService replyService;
    @Autowired private IUserLikeRecordService userLikeRecordService; // 记得创建这个Service

    @RabbitListener(queues = RabbitMQConfig.LIKE_COUNT_QUEUE)
    public void handleLikeMessage(LikeMsgDTO msg) {
        if (msg == null) return;

        // 1. 维护 MySQL 的点赞记录表 (持久化)
        if (msg.getIsLike()) {
            // 插入记录 (如果已存在可能会报错，建议加 try-catch 或 ignore)
            try {
                UserLikeRecord record = new UserLikeRecord();
                record.setUserId(msg.getUserId());
                record.setTargetId(msg.getTargetId());
                record.setType(msg.getType()); // 1或2
                userLikeRecordService.save(record);
            } catch (Exception e) {
            }
        } else {
            // 删除记录
            QueryWrapper<UserLikeRecord> query = new QueryWrapper<>();
            query.eq("user_id", msg.getUserId())
                .eq("target_id", msg.getTargetId())
                .eq("type", msg.getType());
            userLikeRecordService.remove(query);
        }

        // 2. 更新 Redis 里的总数 -> 同步到 MySQL 计数列
        String redisKey = (msg.getType() == 1 ? "review:like:" : "reply:like:") + msg.getTargetId();
        Long size = redisTemplate.opsForSet().size(redisKey);
        int count = size != null ? size.intValue() : 0;

        if (msg.getType() == 1) {
            // 更新影评表
            Review review = new Review();
            review.setId(msg.getTargetId());
            review.setLikeCount(count);
            reviewService.updateById(review);
        } else {
            // 更新回复表
            ReviewReply reply = new ReviewReply();
            reply.setId(msg.getTargetId());
            reply.setLikeCount(count);
            replyService.updateById(reply);
        }
    }
}