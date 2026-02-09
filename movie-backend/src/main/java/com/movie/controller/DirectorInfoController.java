package com.movie.controller;


import org.springframework.beans.factory.annotation.Autowired;
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
import com.movie.dto.DirectorDTO;
import com.movie.entity.DirectorInfo;
import com.movie.service.IDirectorInfoService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

/**
 * <p>
 * 导演信息表 前端控制器
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/directorInfo")
public class DirectorInfoController {
    @Autowired
    private IDirectorInfoService directorInfoService;

    @SysLog("添加导演")
    @PostMapping("/add")
    public Result<String> add(@RequestBody DirectorDTO dto) {
        DirectorInfo director = new DirectorInfo();
        BeanUtil.copyProperties(dto, director); // 属性拷贝
        directorInfoService.save(director);
        return Result.success("添加成功");
    }

    @SysLog("修改导演")
    @PutMapping("/update")
    public Result<String> update(@RequestBody DirectorInfo director) {
        if (director.getId() == null) return Result.error("ID不能为空");
        directorInfoService.updateById(director);
        return Result.success("修改成功");
    }

    @SysLog("删除导演")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        directorInfoService.removeById(id);
        return Result.success("删除成功");
    }

    // 2. 演员列表 (分页查询) - 方便待会儿看数据
    @GetMapping("/list")
    public Result<IPage<DirectorInfo>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String name) {
        Page<DirectorInfo> pageParam = new Page<>(page, size);
        QueryWrapper<DirectorInfo> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            query.like("name", name);
        }
        query.orderByDesc("create_time");
        return Result.success(directorInfoService.page(pageParam, query));
    }
    
    @GetMapping("/{id}")
    public Result<DirectorInfo> getById(@PathVariable Long id) {
        return Result.success(directorInfoService.getById(id));
    }
}
