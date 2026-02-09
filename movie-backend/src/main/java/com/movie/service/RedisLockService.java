package com.movie.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisLockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Lua 脚本：批量原子加锁
    // KEYS: [lock:seat:1:5-5, lock:seat:1:5-6]
    // ARGV: [userId, expiration]
    private static final String LOCK_SCRIPT = 
            "for i, key in ipairs(KEYS) do " +
            "  if redis.call('exists', key) == 1 then " +
            "    return 0; " + // 只要有一个已存在，直接返回失败
            "  end " +
            "end " +
            "for i, key in ipairs(KEYS) do " +
            "  redis.call('set', key, ARGV[1], 'EX', ARGV[2]); " + // 全部加锁
            "end " +
            "return 1;"; // 成功

    /**
     * 原子批量锁座
     */
    public boolean tryLockSeats(Long scheduleId, List<String> seats, Long userId) {
        String lockKeyPrefix = "lock:seat:" + scheduleId + ":";
        List<String> keys = seats.stream()
                .map(seat -> lockKeyPrefix + seat)
                .collect(Collectors.toList());

        // 执行 Lua 脚本 (原子操作)
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LOCK_SCRIPT);
        redisScript.setResultType(Long.class);

        // 锁 15 分钟 (900秒)
        Long result = redisTemplate.execute(redisScript, keys, userId.toString(), "900");

        return result != null && result == 1;
    }

    /**
     * 释放锁
     */
    public void releaseSeatLocks(Long scheduleId, List<String> seats) {
        String lockKeyPrefix = "lock:seat:" + scheduleId + ":";
        List<String> keys = seats.stream()
                .map(seat -> lockKeyPrefix + seat)
                .collect(Collectors.toList());
        redisTemplate.delete(keys);
    }
}