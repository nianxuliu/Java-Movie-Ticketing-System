package com.movie.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.config.RabbitMQConfig;
import com.movie.dto.LikeMsgDTO;
import com.movie.dto.ReviewDTO;
import com.movie.entity.Info;
import com.movie.entity.Review;
import com.movie.entity.ReviewReply;
import com.movie.entity.User;
import com.movie.mapper.ReviewMapper;
import com.movie.service.IMovieService;
import com.movie.service.IReviewReplyService; // 修正引用
import com.movie.service.IReviewService;
import com.movie.utils.UserHolder;
import com.movie.vo.ReplyVO;
import com.movie.vo.ReviewVO;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {

    @Autowired private IMovieService infoService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private RabbitTemplate rabbitTemplate;
    
    
    // 建议注入接口，而不是具体的 Impl 类，防止循环依赖
    @Autowired private IReviewReplyService replyService; 

    // --- 1. 添加评论 (修复 500 错误) ---
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReview(ReviewDTO dto, Long userId) {
        // 1.1 【关键修复】检查是否已经评论过
        long existCount = this.count(new QueryWrapper<Review>()
                .eq("user_id", userId)
                .eq("movie_id", dto.getMovieId()));
        
        if (existCount > 0) {
            // 这里抛出异常，前端会收到 500，但这是业务异常。
            // 更好的做法是自定义一个 BusinessException 返回 400，但为了你现有架构，
            // 这里抛出 RuntimeException，你需要在 Controller 或全局异常处理器里看日志。
            throw new RuntimeException("您已经评价过该电影，无法重复评价！");
        }

        // 1.2 校验评分范围
        if (dto.getScore() == null || dto.getScore().doubleValue() < 0 || dto.getScore().doubleValue() > 10) {
            throw new RuntimeException("评分无效，必须在 0-10 之间");
        }

        // 1.3 保存评论
        Review review = new Review();
        review.setMovieId(dto.getMovieId());
        review.setUserId(userId);
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        review.setLikeCount(0);
        
        boolean saveSuccess = this.save(review);
        if (!saveSuccess) {
            throw new RuntimeException("评论保存失败");
        }

        // 1.4 刷新电影分数
        refreshMovieRating(dto.getMovieId());
    }

    // --- 2. 删除评论 ---
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id) {
        Review review = this.getById(id);
        if (review == null) return;
        
        this.removeById(id);
        
        QueryWrapper<ReviewReply> replyQuery = new QueryWrapper<>();
        replyQuery.eq("review_id", id);
        replyService.remove(replyQuery);

        refreshMovieRating(review.getMovieId());
    }

    // --- 3. 前台列表 ---
    @Override
    public Page<ReviewVO> getReviewsByMovieId(Long movieId, int pageNum, int pageSize) {
        // 1. 先查出影评
        Page<ReviewVO> page = new Page<>(pageNum, pageSize);
        Page<ReviewVO> resultPage = baseMapper.findReviewsByMovieId(page, movieId);

        // 获取当前用户ID (用于检查是否点赞)
        User currentUser = UserHolder.getUser(); 
        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;

        // 2. 填充回复数据
        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            for (ReviewVO vo : resultPage.getRecords()) {
                // 改成 50，一次性查多点，方便前端做"展开"
                Page<ReplyVO> replyPage = replyService.getReplyPage(vo.getId(), 1, 20);
                List<ReplyVO> replies = replyPage.getRecords();
                
                // 【关键修复】必须在这里加载回复的点赞状态！
                // 之前就是因为缺了这行，导致回复的点赞数全是数据库旧值(0)，且状态全灰
                replyService.loadLikeState(replies, currentUserId);
                
                vo.setReplyList(replies);
            }
        }
        
        return resultPage;
    }

    // --- 4. 后台管理员列表 ---
    @Override
    public Page<ReviewVO> getAdminReviewList(int pageNum, int pageSize, String keyword) {
        Page<ReviewVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.findAdminReviews(page, keyword);
    }

    // --- 5. 点赞逻辑 ---
    @Override
    public void likeReview(Long reviewId, Long userId) {
        String key = "review:like:" + reviewId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());
        boolean isLikeAction;

        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.opsForSet().remove(key, userId.toString());
            isLikeAction = false;
        } else {
            redisTemplate.opsForSet().add(key, userId.toString());
            isLikeAction = true;
        }

        LikeMsgDTO msg = new LikeMsgDTO(userId, reviewId, 1, isLikeAction);
        rabbitTemplate.convertAndSend(RabbitMQConfig.LIKE_COUNT_QUEUE, msg);
    }

    // --- 6. 加载点赞状态 ---
    @Override
    public void loadLikeState(List<ReviewVO> records, Long currentUserId) {
        if (records == null || records.isEmpty()) return;
        for (ReviewVO record : records) {
            String key = "review:like:" + record.getId();
            Long likeCount = redisTemplate.opsForSet().size(key);
            record.setLikeCount(likeCount != null ? likeCount.intValue() : 0);

            if (currentUserId != null) {
                Boolean isLiked = redisTemplate.opsForSet().isMember(key, currentUserId.toString());
                record.setIsLiked(Boolean.TRUE.equals(isLiked));
            } else {
                record.setIsLiked(false);
            }
        }
    }

    // ================= 核心优化：使用 SQL 聚合计算评分 =================
    // 你之前的写法查出所有 list 是重大隐患，这里改用 getMap
    private void refreshMovieRating(Long movieId) {
        QueryWrapper<Review> query = new QueryWrapper<>();
        // 直接让数据库计算平均分和总数
        query.select("IFNULL(AVG(score), 0) as avgScore", "COUNT(*) as totalCount");
        query.eq("movie_id", movieId);
        
        Map<String, Object> result = this.getMap(query);
        
        if (result == null) return;

        // 安全转换类型（不同数据库驱动返回类型可能不同，转 string 再转 BigDecimal 最稳）
        BigDecimal avgScore = new BigDecimal(String.valueOf(result.get("avgScore")));
        avgScore = avgScore.setScale(1, RoundingMode.HALF_UP);
        
        @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
        Integer reviewCount = Integer.parseInt(String.valueOf(result.get("totalCount")));

        // 更新电影表
        Info movieToUpdate = new Info();
        movieToUpdate.setId(movieId);
        movieToUpdate.setRating(avgScore);
        movieToUpdate.setReviewCount(reviewCount);
        
        infoService.updateById(movieToUpdate);
    }
}