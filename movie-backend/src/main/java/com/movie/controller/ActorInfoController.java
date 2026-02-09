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
import com.movie.dto.ActorDTO;
import com.movie.entity.ActorInfo;
import com.movie.service.IActorInfoService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/actorInfo")
public class ActorInfoController {

    @Autowired
    private IActorInfoService actorInfoService;

    @SysLog("添加演员")
    @PostMapping("/add")
    public Result<String> add(@RequestBody ActorDTO dto) {
        ActorInfo actor = new ActorInfo();
        BeanUtil.copyProperties(dto, actor); // 属性拷贝
        actorInfoService.save(actor);
        return Result.success("添加成功");
    }

    @SysLog("修改演员")
    @PutMapping("/update")
    public Result<String> update(@RequestBody ActorInfo actor) {
        // ActorInfo 里必须包含 id
        if (actor.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = actorInfoService.updateById(actor);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    @SysLog("删除演员")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 逻辑删除
        boolean success = actorInfoService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    // 4. 演员列表 (支持按名字搜索)
    @GetMapping("/list")
    public Result<IPage<ActorInfo>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String name) {
        Page<ActorInfo> pageParam = new Page<>(page, size);
        QueryWrapper<ActorInfo> query = new QueryWrapper<>();
        // 如果传了 name，就模糊查询
        if (StrUtil.isNotBlank(name)) {
            query.like("name", name);
        }
        query.orderByDesc("create_time"); // 按创建时间倒序
        
        IPage<ActorInfo> result = actorInfoService.page(pageParam, query);
        return Result.success(result);
    }

    // 5. 获取详情 (用于回显)
    @GetMapping("/{id}")
    public Result<ActorInfo> getById(@PathVariable Long id) {
        return Result.success(actorInfoService.getById(id));
    }
}