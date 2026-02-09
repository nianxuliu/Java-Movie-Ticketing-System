package com.movie.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.movie.websocket.WebSocketServer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 引入 RedisLockService
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.config.RabbitMQConfig;
import com.movie.dto.OrderDTO;
import com.movie.dto.PayDTO;
import com.movie.entity.CinemaHall;
import com.movie.entity.CinemaInfo;
import com.movie.entity.Info;
import com.movie.entity.Order;
import com.movie.entity.Schedule;
import com.movie.entity.UserWallet;
import com.movie.entity.WalletLog;
import com.movie.mapper.OrderMapper;
import com.movie.service.ICinemaHallService;
import com.movie.service.ICinemaInfoService;
import com.movie.service.IMovieService;
import com.movie.service.IOrderService;
import com.movie.service.IScheduleService;
import com.movie.service.IUserWalletService;
import com.movie.service.IWalletLogService;
import com.movie.service.RedisLockService;
import com.movie.vo.SeatInfoVO; // 引入 Set

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Autowired
    private IScheduleService scheduleService;
    @Autowired
    private ICinemaHallService hallService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisLockService redisLockService; // 注入新的锁服务
    @Autowired
    private IUserWalletService userWalletService;
    @Autowired
    private IWalletLogService walletLogService;
    @Autowired
    private IMovieService movieService;       // 【新增】用于查电影名
    @Autowired
    private ICinemaInfoService cinemaService;

    // --- 1. 获取座位图 (含坏座处理) ---
    @Override
    @SuppressWarnings("UseSpecificCatch")
    public SeatInfoVO getSeatInfo(Long scheduleId) throws Exception {
        // 1. 查询排片信息
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new RuntimeException("该排片场次不存在或已下架");
        }

        // 2. 【新增】查询关联的 电影、影院、影厅 信息
        // 前端右侧卡片需要这些数据，否则会显示空白或默认值
        Info movie = movieService.getById(schedule.getMovieId());
        CinemaInfo cinema = cinemaService.getById(schedule.getCinemaId());
        CinemaHall hall = hallService.getById(schedule.getHallId());

        if (hall == null || movie == null || cinema == null) {
            throw new RuntimeException("排片关联信息缺失(影厅/电影/影院)");
        }

        // 3. 解析影厅座位配置 (JSON -> Map)
        Map<String, Object> hallConfig = null;
        try {
            hallConfig = objectMapper.readValue(
                hall.getSeatConfig(), 
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            // 容错处理：如果解析失败，给个默认空对象，防止整个页面打不开
            hallConfig = new HashMap<>();
            hallConfig.put("rows", 0);
            hallConfig.put("cols", 0);
        }

        // 4. 安全提取坏座 (broken_seats)
        // 解决 unchecked 警告，先判断是否存在且类型是否正确
        List<String> brokenSeats = new ArrayList<>();
        if (hallConfig.containsKey("broken_seats")) {
            Object brokenObj = hallConfig.get("broken_seats");
            if (brokenObj instanceof List) {
                // 安全转换
                List<?> list = (List<?>) brokenObj;
                for (Object item : list) {
                    brokenSeats.add(item.toString());
                }
            }
        }

        // 5. 查询【已售出】座位 (从数据库)
        // 逻辑：查询该场次下，状态为 "已支付(1)" 或 "已观影(4)" 的订单
        // 如果你有 "待支付(0)" 但没存 Redis 的逻辑，也要加上 0
        QueryWrapper<Order> soldQuery = new QueryWrapper<>();
        soldQuery.eq("schedule_id", scheduleId)
                .in("status", 1, 4); // 假设 1=已支付, 4=已完成。具体看你的状态定义
             // .ne("status", 2); // 或者直接查“不等于已取消”的所有订单

        List<Order> soldOrders = this.list(soldQuery);
    
        // 将订单里的座位号字符串 "1-1,1-2" 拆分并收集
        List<String> finalSoldSeats = soldOrders.stream()
                .filter(o -> o.getSeatInfo() != null)
                .flatMap(order -> Arrays.stream(order.getSeatInfo().split(",")))
                .collect(Collectors.toList());

        // 6. 查询【锁定中】座位 (从 Redis)
        // 这里的 Key 格式必须和 createOrder 存入 Redis 时保持一致
        String lockKeyPattern = "lock:seat:" + scheduleId + ":*";
        Set<String> lockedKeys = redisTemplate.keys(lockKeyPattern);
    
        if (lockedKeys != null && !lockedKeys.isEmpty()) {
            List<String> lockedSeats = lockedKeys.stream()
                    // 假设 key 是 "lock:seat:101:5-6"，截取最后的 "5-6"
                    .map(key -> key.substring(key.lastIndexOf(":") + 1))
                    .collect(Collectors.toList());
            finalSoldSeats.addAll(lockedSeats);
        }

        // 7. 组装 VO 返回
        SeatInfoVO vo = new SeatInfoVO();
    
        // --- 填充基础座位信息 ---
        vo.setHallConfig(hallConfig);
        vo.setBrokenSeats(brokenSeats);
        vo.setSoldSeats(finalSoldSeats.stream().distinct().collect(Collectors.toList())); // 去重

        // --- 【关键】填充右侧详情信息 ---
        vo.setMovieTitle(movie.getTitle());
        vo.setCinemaName(cinema.getName());
        vo.setHallName(hall.getName());
        vo.setPrice(schedule.getPrice());
        vo.setStartTime(schedule.getStartTime());

        return vo;
    }
    

    // --- 锁座并创建订单 ---
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderDTO dto, Long userId) {
        // --- 1. 数据清洗与格式验证 ---
        List<String> rawSeats = dto.getSeats();
        if (rawSeats == null || rawSeats.isEmpty()) {
            throw new RuntimeException("请选择座位");
        }

        Set<String> distinctSet = new HashSet<>();
        for (String seatStr : rawSeats) {
            // 防御性编程：以逗号分割，防止前端传 ["5-6,5-7"] 这种怪异格式
            String[] splitSeats = seatStr.split(",");
            for (String s : splitSeats) {
                // 去除空格 " 5-6 " -> "5-6"
                String cleanSeat = s.trim();
                if (cleanSeat.isEmpty()) {
                    continue;
                }
                if (distinctSet.contains(cleanSeat)) {
                    throw new RuntimeException("订单中包含重复座位: " + cleanSeat);
                }
                distinctSet.add(cleanSeat);
            }
        }

        if (distinctSet.isEmpty()) {
            throw new RuntimeException("有效座位为空");
        }

        // 转回 List，确保后续所有逻辑都使用这个清洗过的 finalSeats
        List<String> finalSeats = new ArrayList<>(distinctSet);

        // --- 2. 尝试原子锁座 (使用 Lua 脚本) ---
        // 使用 finalSeats 生成 Key，这样就是标准的 lock:seat:4:5-6 了
        boolean lockSuccess = redisLockService.tryLockSeats(dto.getScheduleId(), finalSeats, userId);
        if (!lockSuccess) {
            throw new RuntimeException("部分座位已被锁定，请重新选择");
        }

        try {
            // 3. 校验排片是否存在
            Schedule schedule = scheduleService.getById(dto.getScheduleId());
            if (schedule == null) {
                throw new RuntimeException("排片不存在");
            }
            // 如果 (开场时间 < 当前时间)，说明已经开演了，不能卖票
            if (schedule.getStartTime().isBefore(LocalDateTime.now())) {
                redisLockService.releaseSeatLocks(dto.getScheduleId(), finalSeats);
                throw new RuntimeException("电影已开场，停止售票！");
            }

            CinemaHall hall = hallService.getById(schedule.getHallId());


            if (hall == null) {
                throw new RuntimeException("关联影厅不存在");
            }

            // --- 检查是否选了坏座 ---
            Map<String, Object> config;
            try {
                config = objectMapper.readValue(
                    hall.getSeatConfig(), 
                    new TypeReference<Map<String, Object>>() {}
                );
            } catch (JsonProcessingException e) {
                // 如果解析失败，说明数据库里的 JSON 格式不对
                // 【关键步骤】必须释放刚才锁住的座位！
                redisLockService.releaseSeatLocks(dto.getScheduleId(), finalSeats);
                throw new RuntimeException("影厅座位数据异常，无法下单");
            }
            
            // 提取坏座列表
            List<String> brokenSeats = new ArrayList<>();
            if (config.containsKey("broken_seats")) {
                brokenSeats = (List<String>) config.get("broken_seats");
            }

            int maxRow = (int) config.get("rows");
            int maxCol = (int) config.get("cols");

            for (String seat : finalSeats) {
                // 1. 检查是否是坏座
                if (brokenSeats.contains(seat)) {
                    throw new RuntimeException("座位 " + seat + " 是损坏座位，无法购买");
                }

                // 2. 检查边界 (之前的逻辑)
                String[] parts = seat.split("-");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                if (row < 1 || row > maxRow || col < 1 || col > maxCol) {
                    throw new RuntimeException("座位 " + seat + " 不存在");
                }
            }

            // 4. 双重检查数据库 (已支付的作为兜底)
            QueryWrapper<Order> checkQuery = new QueryWrapper<>();
            checkQuery.eq("schedule_id", dto.getScheduleId())
                    .eq("status", 1) // 已支付
                    .in("seat_info", finalSeats); // MybatisPlus 会自动处理 List in ('5-6', '5-7')

            if (this.count(checkQuery) > 0) {
                throw new RuntimeException("部分座位已售出");
            }

            // 5. 计算金额 (现在 finalSeats.size() 是真实的座位数了)
            BigDecimal price = schedule.getPrice();
            BigDecimal count = new BigDecimal(finalSeats.size());
            BigDecimal totalPrice = price.multiply(count);

            // 6. 创建订单
            Order order = new Order();
            String orderNo = IdUtil.getSnowflakeNextIdStr();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setScheduleId(dto.getScheduleId());
            // 存入数据库的是标准格式 "5-6,5-7"
            order.setSeatInfo(String.join(",", finalSeats));
            order.setTotalPrice(totalPrice);
            order.setStatus(0);
            this.save(order);

            // 7. 发送延迟消息
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_TTL_QUEUE, orderNo);

            return orderNo;

        } catch (RuntimeException e) {
            // 失败释放锁
            redisLockService.releaseSeatLocks(dto.getScheduleId(), finalSeats);
            throw e; // 继续抛出异常给Controller
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【核心】开启事务：只要下面任何一行报错，所有数据库操作自动回滚！
    public void payOrder(PayDTO dto, Long userId) {
        // 1. 查询并校验订单
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("order_no", dto.getOrderNo()).eq("user_id", userId);
        Order order = this.getOne(query);

        if (order == null) {
            throw new RuntimeException("订单不存在或不属于当前用户");
        }
        if (order.getStatus() != 0) { // 0-待支付
            throw new RuntimeException("订单状态异常，无法支付"); // 抛出异常 -> 触发回滚
        }

        // 2. 查询用户钱包
        QueryWrapper<UserWallet> walletQuery = new QueryWrapper<>();
        walletQuery.eq("user_id", userId);
        UserWallet wallet = userWalletService.getOne(walletQuery);
        if (wallet == null) {
            throw new RuntimeException("用户钱包不存在"); // 抛出异常 -> 触发回滚
        }

        // 3. 【核心】校验是否设置了支付密码
        if (wallet.getPayPassword() == null) {
            throw new RuntimeException("未设置支付密码，请先前往设置！");
        }

        // 4. 【核心】校验支付密码是否正确
        if (!BCrypt.checkpw(dto.getPayPassword(), wallet.getPayPassword())) {
            throw new RuntimeException("支付密码错误"); // 抛出异常 -> 触发回滚
        }

        // 5. 【核心】余额校验 (Balance Check)
        // wallet.getBalance() < order.getTotalPrice()
        if (wallet.getBalance().compareTo(order.getTotalPrice()) < 0) {
            throw new RuntimeException("余额不足，请充值！"); // 抛出异常 -> 触发回滚
        }

        // --- 到这里说明一切正常，开始执行扣款和更新 ---
        try {
            // 6. 扣减余额
            BigDecimal newBalance = wallet.getBalance().subtract(order.getTotalPrice());
            wallet.setBalance(newBalance);

            // 这里利用了 MyBatis-Plus 的乐观锁插件（如果配置了 @Version），可以防止并发扣款
            boolean updateWalletSuccess = userWalletService.updateById(wallet);
            if (!updateWalletSuccess) {
                throw new RuntimeException("扣款失败，请重试"); // 并发冲突时回滚
            }

            // 7. 修改订单状态为“已支付”
            order.setStatus(1); // 1-已支付
            order.setPayTime(LocalDateTime.now());
            boolean updateOrderSuccess = this.updateById(order);
            if (!updateOrderSuccess) {
                throw new RuntimeException("更新订单状态失败"); // 回滚钱包扣款
            }

            // 8. 支付成功，释放 Redis 座位锁
            // 因为座位已经永久属于用户了（存入数据库了），不再需要 Redis 的临时锁
            Schedule schedule = scheduleService.getById(order.getScheduleId());

            if (schedule != null) {
                redisLockService.releaseSeatLocks(schedule.getId(), List.of(order.getSeatInfo().split(",")));
            }

            // --- 新增：推送通知 ---
            WebSocketServer.sendInfo(userId.toString(), " 支付成功！祝您观影愉快！");

            // 8. 记录支出流水
            WalletLog walletLog = new WalletLog(); 
            walletLog.setUserId(userId);
            walletLog.setAmount(order.getTotalPrice().negate()); 
            walletLog.setType((byte) 2); // 2-购票
            walletLog.setOrderNo(order.getOrderNo());
            walletLog.setRemark("购买电影票: " + order.getSeatInfo());
            walletLogService.save(walletLog);

        } catch (RuntimeException e) {
            // 捕获所有未知异常，手动抛出 RuntimeException 以确保事务回滚
            throw new RuntimeException("支付过程中发生错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【核心】开启事务：钱和状态必须同时成功
    public void refundOrder(String orderNo, Long userId) {
        // 1. 查询并校验订单
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("order_no", orderNo).eq("user_id", userId);
        Order order = this.getOne(query);

        if (order == null) {
            throw new RuntimeException("订单不存在或不属于当前用户");
        }

        // 只有 "已支付(1)" 的订单才能退款
        // 待支付(0)的应该走取消流程，已取消(2)或已退款(3)的不能重复退
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态异常，无法退款（仅已支付订单可退）");
        }

        // --- 新增：核心时间校验 ---
        Schedule schedule = scheduleService.getById(order.getScheduleId());
        // 如果电影已经开始了，就不让退了 (通常规定开场前15分钟不能退，这里简单点，开场后不能退)
        if (schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("电影已开场，无法退票");
        }
        // -------------------------

        // 2. 查询用户钱包
        QueryWrapper<UserWallet> walletQuery = new QueryWrapper<>();
        walletQuery.eq("user_id", userId);
        UserWallet wallet = userWalletService.getOne(walletQuery);
        if (wallet == null) {
            throw new RuntimeException("用户钱包不存在");
        }

        try {
            // 3. 【核心】执行退款：余额加回去
            // balance = balance + totalPrice
            BigDecimal refundAmount = order.getTotalPrice();
            BigDecimal newBalance = wallet.getBalance().add(refundAmount);
            wallet.setBalance(newBalance);

            boolean updateWalletSuccess = userWalletService.updateById(wallet);
            if (!updateWalletSuccess) {
                throw new RuntimeException("退款入账失败");
            }

            // 4. 【核心】修改订单状态
            order.setStatus(3); // 3-已退款
            // (可选) 可以加一个 refund_time 字段记录退款时间，这里为了简化省略
            boolean updateOrderSuccess = this.updateById(order);
            if (!updateOrderSuccess) {
                throw new RuntimeException("更新订单状态失败"); // 回滚钱包
            }

            // 5. 【兜底】清理 Redis 锁
            // 虽然支付成功时应该已经释放了锁，但为了防止当时释放失败导致死锁，
            // 这里再执行一次释放操作，确保座位彻底回归自由。
            
            redisLockService.releaseSeatLocks(schedule.getId(), List.of(order.getSeatInfo().split(",")));
            
            WebSocketServer.sendInfo(userId.toString(), " 退款成功！金额已返回钱包。");
        
            // 6. 记录收入流水
            WalletLog walletLog = new WalletLog();
            walletLog.setUserId(userId);
            walletLog.setAmount(order.getTotalPrice()); // 收入为正
            walletLog.setType((byte) 3); // 3-退款
            walletLog.setOrderNo(order.getOrderNo());
            walletLog.setRemark("订单退款");
            walletLogService.save(walletLog);

        } catch (RuntimeException e) {
            // 捕获异常，抛出 RuntimeException 触发回滚
            throw new RuntimeException("退款失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        // 1. 查询订单
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("order_no", orderNo).eq("user_id", userId);
        Order order = this.getOne(query);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 只有“待支付”的订单可以手动取消
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态已改变，无法取消");
        }

        try {
            // 3. 修改状态为“已取消”
            order.setStatus(2); // 2-已取消
            // (可选) setCancelTime
            this.updateById(order);

            // 4. 【核心】立刻释放 Redis 座位锁
            // 这样别人就能马上买这几个座了，不用等15分钟
            Schedule schedule = scheduleService.getById(order.getScheduleId());
            if (schedule != null) {
                redisLockService.releaseSeatLocks(schedule.getId(), List.of(order.getSeatInfo().split(",")));
            }
            WebSocketServer.sendInfo(userId.toString(), " 订单取消成功！");

        } catch (Exception e) {
            throw new RuntimeException("取消订单失败: " + e.getMessage());
        }
    }
}
