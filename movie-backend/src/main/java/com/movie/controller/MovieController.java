package com.movie.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.doc.MovieDoc;
import com.movie.dto.MovieDTO;
import com.movie.dto.SearchDTO;
import com.movie.entity.Info;
import com.movie.service.IMovieService;
import com.movie.vo.MovieDetailVO;

import cn.hutool.core.util.StrUtil;

/**
 * <p>
 * 电影信息表 前端控制器
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/movie")
public class MovieController {
    @Autowired
    private IMovieService infoService;
    @Autowired
    private StringRedisTemplate redisTemplate; // 记得注入
    @Autowired
    private ObjectMapper objectMapper;

    @SysLog("发布电影")
    @PostMapping("/add")
    @SuppressWarnings("CallToPrintStackTrace")
    public Result<String> add(@RequestBody MovieDTO dto) {
        try {
            infoService.addMovie(dto);
            return Result.success("电影发布成功");
        } catch (Exception e) {
            e.printStackTrace(); // 方便调试看报错
            return Result.error("发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    public Result<Page<MovieDoc>> search(@RequestBody SearchDTO dto) {
        Page<MovieDoc> resultPage = infoService.search(dto);
        return Result.success(resultPage);
    }

    @SysLog("修改电影")
    @PutMapping("/update")
    public Result<String> update(@RequestBody MovieDTO dto) {
        try {
            infoService.updateMovie(dto);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @SysLog("删除电影")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            infoService.removeMovie(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    // 电影详情 (用于回显数据)
    @GetMapping("/{id}")
    public Result<Info> getById(@PathVariable Long id) {
        return Result.success(infoService.getById(id));
    }

    // 首页热门电影 (Top 20) - 带 Redis 缓存
    @GetMapping("/top20") // 建议改为 top20，对应前端逻辑
    @SuppressWarnings("CallToPrintStackTrace")
    public Result<List<Info>> getTop20() {
        // 1. 修改 Key 名字，避免和之前的 10 部电影缓存混淆
        String key = "movie:top20"; 

        // 1. 先查 Redis
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.isEmpty()) {
            try {
                // 反序列化 List
                List<Info> list = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Info>>() {});
                return Result.success(list);
            } catch (JsonProcessingException e) {
                e.printStackTrace(); // 解析失败就去查库
            }
        }

        // 2. Redis 没命中，查数据库
        QueryWrapper<Info> query = new QueryWrapper<>();
        query.orderByDesc("rating") // 按评分倒序
             .last("LIMIT 20");     // 关键修改：只取前 20 部

        List<Info> topMovies = infoService.list(query);

        // 3. 写入 Redis (过期时间 30 分钟)
        try {
            String newJson = objectMapper.writeValueAsString(topMovies);
            redisTemplate.opsForValue().set(key, newJson, 30, java.util.concurrent.TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return Result.success(topMovies);
    }

    @GetMapping("/detail/{id}") // 换个路径，区分原来的简单查询
    public Result<MovieDetailVO> getDetail(@PathVariable Long id) {
        try {
            // 需要去 InfoServiceImpl 里把上面的方法加上，并在 Interface 里定义
            return Result.success(infoService.getMovieDetail(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Info>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        // 核心修复：使用全限定名来创建 MyBatis-Plus 的 Page 对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Info> pageParam = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            
        QueryWrapper<Info> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            query.like("title", name);
        }
        query.orderByDesc("release_date");

        return Result.success(infoService.page(pageParam, query));
    }

     // 全量同步数据到 ES (仅管理员可用)
    @GetMapping("/sync")
    @SuppressWarnings("CallToPrintStackTrace")
    public Result<String> sync() {
        long start = System.currentTimeMillis();
        try {
            infoService.syncEsData();
            long end = System.currentTimeMillis();
            return Result.success("同步成功，耗时: " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("同步失败: " + e.getMessage());
        }
    }

}
