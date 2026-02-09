package com.movie.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.entity.Review;
import com.movie.vo.ReviewVO;

public interface ReviewMapper extends BaseMapper<Review> {

    // 自定义分页连表查询
    @Select("SELECT r.*, u.nickname, u.avatar_url " +
            "FROM movie_review r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.movie_id = #{movieId} AND r.is_deleted = 0 " +
            "ORDER BY r.create_time DESC")
    Page<ReviewVO> findReviewsByMovieId(Page<ReviewVO> page, @Param("movieId") Long movieId);

    // 使用 <script> 标签支持动态 SQL，如果 keyword 不为空，则搜索 content 或 nickname
    @Select("<script>" +
            "SELECT r.*, u.nickname, u.avatar_url, m.title as movie_title " +
            "FROM movie_review r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN movie_info m ON r.movie_id = m.id " +
            "WHERE r.is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (r.content LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY r.create_time DESC" +
            "</script>")
    Page<ReviewVO> findAdminReviews(Page<ReviewVO> page, @Param("keyword") String keyword);

    @Update("UPDATE movie_review SET like_count = like_count + 1 WHERE id = #{id}")
    void increaseLikeCount(Long id);

    @Update("UPDATE movie_review SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    void decreaseLikeCount(Long id);
}