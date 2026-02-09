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
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.ReplyDTO;
import com.movie.entity.ReviewReply;
import com.movie.entity.User;
import com.movie.mapper.ReviewReplyMapper;
import com.movie.service.IReviewReplyService;
import com.movie.service.impl.ReviewReplyServiceImpl;
import com.movie.utils.UserHolder;
import com.movie.vo.ReplyVO;

@RestController
@RequestMapping("/reply")
public class ReviewReplyController {

    @Autowired
    private IReviewReplyService replyService;
    @Autowired
    private ReviewReplyMapper replyMapper; // 简单起见直接调Mapper，规范应写在Service
    // @Autowired
    // private StringRedisTemplate redisTemplate;

    // 发表回复
    @PostMapping("/add")
    public Result<String> add(@RequestBody ReplyDTO dto) {
        User user = UserHolder.getUser();
        if (user == null) return Result.error("请先登录");

        ReviewReply reply = new ReviewReply();
        reply.setReviewId(dto.getReviewId());
        reply.setUserId(user.getId());
        reply.setTargetUserId(dto.getTargetUserId());
        reply.setContent(dto.getContent());
        
        replyService.save(reply);
        return Result.success("回复成功");
    }

    // 点赞回复
    @PostMapping("/like/{replyId}")
    public Result<String> like(@PathVariable Long replyId) {
        User user = UserHolder.getUser();
        if (user == null) return Result.error("请先登录");
        // 需要去 Service 补上接口定义
        ((ReviewReplyServiceImpl) replyService).likeReply(replyId, user.getId());
        return Result.success("操作成功");
    }

    // 获取某条评论的回复列表 (分页版)
    @GetMapping("/list/{reviewId}")
    public Result<Page<ReplyVO>> list(@PathVariable Long reviewId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        
        // 1. 构建分页对象
        Page<ReplyVO> pageParam = new Page<>(page, size);
        
        // 2. 调用 Mapper (MP 会自动拦截并生成 LIMIT 语句)
        Page<ReplyVO> result = replyMapper.findRepliesByReviewId(pageParam, reviewId);
        User user = UserHolder.getUser();
        if (user != null) {
            ((ReviewReplyServiceImpl) replyService).loadLikeState(result.getRecords(), user.getId());
        }
        
        return Result.success(result);
    }

    // 删除回复 (管理员/用户自己)
    @SysLog("删除回复")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 1. 检查是否存在
        ReviewReply reply = replyService.getById(id);
        if (reply == null) {
            return Result.error("回复不存在");
        }

        // 2. 权限校验 (可选：如果是普通用户调用，只能删自己的；如果是管理员，可以删所有)
        // 为了课设简化，我们假设这个接口是管理员后台调用的，或者前端做了按钮隐藏
        // User user = UserHolder.getUser();
        // if (!user.getIsAdmin() && !reply.getUserId().equals(user.getId())) { ... }

        // 3. 执行删除 (逻辑删除)
        boolean success = replyService.removeById(id);
        
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}