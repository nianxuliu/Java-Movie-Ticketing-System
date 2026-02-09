package com.movie.mapper;


import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.entity.ReviewReply;
import com.movie.vo.ReplyVO;

public interface ReviewReplyMapper extends BaseMapper<ReviewReply> {

    // 查询某条评论下的所有回复 (连表查用户)
    @Select("SELECT r.*, u.nickname, u.avatar_url, tu.nickname as target_nickname " +
            "FROM review_reply r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_user tu ON r.target_user_id = tu.id " +
            "WHERE r.review_id = #{reviewId} AND r.is_deleted = 0 " +
            "ORDER BY r.create_time ASC")
    Page<ReplyVO> findRepliesByReviewId(Page<ReplyVO> page, @Param("reviewId") Long reviewId);
}