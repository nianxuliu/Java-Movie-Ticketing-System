package com.movie.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.dto.HallDTO;
import com.movie.entity.CinemaHall;
import com.movie.entity.Schedule;
import com.movie.service.ICinemaHallService;
import com.movie.service.ICinemaInfoService;
import com.movie.service.IScheduleService;

@RestController
@RequestMapping("/hall")
public class CinemaHallController {

    @Autowired
    private ICinemaHallService hallService;
    @Autowired
    private ICinemaInfoService cinemaInfoService; // 用于校验影院是否存在
    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 自带的 JSON 工具
    @Autowired
    private IScheduleService scheduleService; // 注入排片服务

    @SysLog("添加影厅")
    @PostMapping("/add")
    @SuppressWarnings("UseSpecificCatch")
    public Result<String> add(@RequestBody HallDTO dto) {
        // 校验影院是否存在
        if (cinemaInfoService.getById(dto.getCinemaId()) == null) {
            return Result.error("所属影院不存在！");
        }

        CinemaHall hall = new CinemaHall();
        hall.setCinemaId(dto.getCinemaId());
        hall.setName(dto.getName());

        // --- 核心修复：统一改为解析 JSON，支持保存坏座 ---
        try {
            // 尝试解析 JSON，例如 {"rows":10, "cols":12, "broken_seats":["1-1"]}
            JsonNode node = objectMapper.readTree(dto.getSeatConfig());
            
            if (!node.has("rows") || !node.has("cols")) {
                return Result.error("配置必须包含 rows 和 cols");
            }
            
            // 直接把前端传来的标准 JSON 存进去，这样坏座信息(broken_seats)也就自动存进去了
            hall.setSeatConfig(dto.getSeatConfig());
            
        } catch (JsonProcessingException e) {
            // 如果解析 JSON 失败，尝试兼容旧的 "10x8" 格式（防止旧接口报错）
            try {
                String[] parts = dto.getSeatConfig().split("x");
                int rows = Integer.parseInt(parts[0]);
                int cols = Integer.parseInt(parts[1]);
                ObjectNode seatJson = objectMapper.createObjectNode();
                seatJson.put("rows", rows);
                seatJson.put("cols", cols);
                hall.setSeatConfig(seatJson.toString());
            } catch (Exception ex) {
                return Result.error("座位配置格式错误，请检查前端传参");
            }
        }

        try {
            hallService.save(hall);
            return Result.success("影厅添加成功");
        } catch (DuplicateKeyException e) {
            return Result.error("该影院下已存在同名影厅！");
        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @SysLog("修改影厅")
    @PutMapping("/update")
    @SuppressWarnings("UseSpecificCatch")
    public Result<String> update(@RequestBody CinemaHall hall) {
        if (hall.getId() == null) return Result.error("ID不能为空");

        try {
            // --- 核心逻辑：座位配置 JSON 强校验 (含坏座) ---
            String configStr = hall.getSeatConfig();
            
            if (configStr != null && !configStr.trim().isEmpty()) {
                try {
                    JsonNode node = objectMapper.readTree(configStr);
                    
                    // 1. 基础校验
                    if (!node.has("rows") || !node.has("cols")) {
                        throw new RuntimeException("配置必须包含 'rows' 和 'cols'");
                    }
                    int rows = node.get("rows").asInt();
                    int cols = node.get("cols").asInt();
                    if (rows <= 0 || cols <= 0) throw new RuntimeException("行数列数必须大于0");

                    // 2. 坏座校验 (如果有 broken_seats 字段)
                    if (node.has("broken_seats")) {
                        JsonNode brokenNode = node.get("broken_seats");
                        if (!brokenNode.isArray()) {
                            throw new RuntimeException("'broken_seats' 必须是数组格式");
                        }
                        // 遍历检查每个坏座坐标是否合法
                        for (JsonNode seatNode : brokenNode) {
                            String seat = seatNode.asText();
                            if (!seat.matches("^\\d+-\\d+$")) {
                                throw new RuntimeException("坏座格式错误: " + seat);
                            }
                            String[] parts = seat.split("-");
                            int r = Integer.parseInt(parts[0]);
                            int c = Integer.parseInt(parts[1]);
                            if (r < 1 || r > rows || c < 1 || c > cols) {
                                throw new RuntimeException("坏座 " + seat + " 越界");
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("JSON 格式错误");
                }
            }

            hallService.updateById(hall);
            return Result.success("修改成功");
            
        } catch (Exception e) {
            return Result.error("修改失败: " + e.getMessage());
        }
    }

    @SysLog("删除影厅")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        QueryWrapper<Schedule> scheduleQuery = new QueryWrapper<>();
        scheduleQuery.eq("hall_id", id);
        long scheduleCount = scheduleService.count(scheduleQuery);
        
        if (scheduleCount > 0) {
            return Result.error("该影厅下有排片记录，禁止删除！请先清理排片。");
        }

        hallService.removeById(id);
        return Result.success("删除成功");
    }

    // 4. 查询某影院下的所有影厅
    @GetMapping("/list/{cinemaId}")
    public Result<List<CinemaHall>> listByCinema(@PathVariable Long cinemaId) {
        QueryWrapper<CinemaHall> query = new QueryWrapper<>();
        query.eq("cinema_id", cinemaId);
        return Result.success(hallService.list(query));
    }

    @GetMapping("/list/all")
    public Result<List<CinemaHall>> listAll() {
        return Result.success(hallService.list());
    }
}