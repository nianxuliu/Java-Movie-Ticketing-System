package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.common.Result;
import com.movie.dto.ReviewDTO;
import com.movie.entity.User;
import com.movie.service.IReviewService;
import com.movie.utils.UserHolder;
import com.movie.vo.ReviewVO;

/**
 * <p>
 * 影评表 前端控制器
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/review")
public class ReviewController {
    @Autowired
    private IReviewService reviewService;

    // 发表评论
    @PostMapping("/add")
    public Result<String> add(@RequestBody ReviewDTO dto) {
        User user = UserHolder.getUser();
        if (user == null) {
            return Result.error("用户未登录");
        }

        try {
            reviewService.addReview(dto, user.getId());
            return Result.success("评论成功");
        } catch (RuntimeException e) {
            // 【核心修复】捕获 Service 抛出的 "您已经评价过..." 异常
            // 将其转为 200 OK 但 success=false 的 Result 返回给前端
            return Result.error(e.getMessage());
        }
    }

    // 获取电影的评论列表
    @GetMapping("/list/{movieId}")
    public Result<Page<ReviewVO>> getReviews(@PathVariable Long movieId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        // 1. 查出分页数据
        Page<ReviewVO> reviewPage = reviewService.getReviewsByMovieId(movieId, page, size);
        
        // 2. 判断当前用户是否登录，如果登录了，就加载点赞状态
        User user = UserHolder.getUser();
        if (user != null) {
            reviewService.loadLikeState(reviewPage.getRecords(), user.getId());
        }
        
        return Result.success(reviewPage);
    }

    // 点赞接口 (需要登录)
    @PostMapping("/like/{reviewId}")
    public Result<String> like(@PathVariable Long reviewId) {
        User user = UserHolder.getUser();
        if (user == null) return Result.error("请先登录");

        reviewService.likeReview(reviewId, user.getId());
        return Result.success("操作成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // 管理员查看列表
    @GetMapping("/admin/list")
    public Result<Page<ReviewVO>> adminList(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        Page<ReviewVO> result = reviewService.getAdminReviewList(page, size, keyword);
        return Result.success(result);
    }
}
