package com.movie.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.entity.ReviewReply;
import com.movie.vo.ReplyVO;

/**
 * <p>
 * 评论回复表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IReviewReplyService extends IService<ReviewReply> {
    Page<ReplyVO> getReplyPage(Long reviewId, int page, int size);
    void loadLikeState(List<ReplyVO> list, Long userId);
}
