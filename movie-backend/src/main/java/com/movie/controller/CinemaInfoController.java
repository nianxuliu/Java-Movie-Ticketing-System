package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.CinemaDTO;
import com.movie.entity.CinemaHall;
import com.movie.entity.CinemaInfo;
import com.movie.service.ICinemaHallService;
import com.movie.service.ICinemaInfoService;

import cn.hutool.core.bean.BeanUtil;

@RestController
@RequestMapping("/cinema")
public class CinemaInfoController {

    @Autowired
    private ICinemaInfoService cinemaInfoService;
    @Autowired
    private ICinemaHallService hallService;

    @SysLog("添加影院")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CinemaDTO dto) {
        CinemaInfo cinema = new CinemaInfo();
        BeanUtil.copyProperties(dto, cinema);
        
        try {
            cinemaInfoService.save(cinema);
            return Result.success("影院添加成功");
        } catch (DuplicateKeyException e) {
            // 当数据库的 uk_city_name 唯一约束被触发时，会抛出这个异常
            return Result.error("该城市已存在同名影院，请勿重复添加！");
        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @SysLog("修改影院")
    @PutMapping("/update")
    public Result<String> update(@RequestBody CinemaInfo cinema) {
        if (cinema.getId() == null) return Result.error("ID不能为空");
        try {
            cinemaInfoService.updateById(cinema);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error("修改失败(可能重名): " + e.getMessage());
        }
    }

    @SysLog("删除影院")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 1. 检查该影院下是否有影厅
        QueryWrapper<CinemaHall> hallQuery = new QueryWrapper<>();
        hallQuery.eq("cinema_id", id);
        long hallCount = hallService.count(hallQuery);
        
        if (hallCount > 0) {
            return Result.error("该影院下仍有影厅，禁止删除！请先删除影厅。");
        }

        // 2. (可选) 检查是否有排片 (虽然删了影厅肯定没排片，但为了双保险可以查一下 schedule 表)
        
        cinemaInfoService.removeById(id);
        return Result.success("删除成功");
    }

    // 4. 影院列表 (支持按城市/名称搜索)
    @GetMapping("/list")
    public Result<IPage<CinemaInfo>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(required = false) String name) {
        Page<CinemaInfo> pageParam = new Page<>(page, size);
        QueryWrapper<CinemaInfo> query = new QueryWrapper<>();
        if (city != null && !city.isEmpty()) query.eq("city", city);
        if (name != null && !name.isEmpty()) query.like("name", name);
        
        return Result.success(cinemaInfoService.page(pageParam, query));
    }
    
    // 5. 详情
    @GetMapping("/{id}")
    public Result<CinemaInfo> getById(@PathVariable Long id) {
        return Result.success(cinemaInfoService.getById(id));
    }
}