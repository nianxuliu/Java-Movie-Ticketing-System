package com.movie.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.dto.ReviewDTO;
import com.movie.entity.Review;
import com.movie.vo.ReviewVO;

/**
 * <p>
 * 影评表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IReviewService extends IService<Review> {
    void addReview(ReviewDTO dto, Long userId); // 增加一个 userId 参数

    public Page<ReviewVO> getReviewsByMovieId(Long movieId, int page, int size);

    public void likeReview(Long reviewId, Long userId);

    public void loadLikeState(List<ReviewVO> records, Long currentUserId);

     // 管理员分页查询
    Page<ReviewVO> getAdminReviewList(int page, int size, String keyword);
    
    // 删除评论（带业务逻辑）
    void deleteReview(Long id);

}
