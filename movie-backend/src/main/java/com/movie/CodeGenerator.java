package com.movie;

import java.util.Collections;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

/**
 * 代码生成器
 * 运行 main 方法即可一键生成 Entity, Mapper, Service, Controller
 */
public class CodeGenerator {

    public static void main(String[] args) {
        // 1. 数据库配置
        String url = "jdbc:mysql://localhost:3306/movie_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";     // <--- 【检查你的数据库账号】
        String password = "374629";     // <--- 【检查你的数据库密码】

        // 2. 开始生成
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("Liu") // 设置作者
                            .enableSwagger() // 开启 swagger 模式 (可选)
                            .outputDir(System.getProperty("user.dir") + "/movie-backend/src/main/java");
                })
                .packageConfig(builder -> {
                    builder.parent("com.movie") // 设置父包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/movie-backend/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    // 在这里列出所有需要生成的表名，用逗号分隔
                                    "sys_user", "movie_info", "actor_info", "director_info",
                                    "movie_actor", "movie_director", "movie_review",
                                    "cinema_info", "cinema_hall", "movie_schedule", "movie_order",
                                    "review_reply", "user_like_record",
                                    "user_wallet", "wallet_log",
                                    "sys_role", "sys_user_role", "sys_log"
                            )
                            .addTablePrefix("sys_", "movie_") // 设置过滤表前缀(生成的类名不会带 sys_)
                            
                            // Entity 策略
                            .entityBuilder()
                            .enableLombok() // 开启 Lombok
                            .enableTableFieldAnnotation() // 开启字段注解
                            .logicDeleteColumnName("is_deleted") // 逻辑删除字段
                            
                            // Controller 策略
                            .controllerBuilder()
                            .enableRestStyle(); // 开启 @RestController
                })
                .templateEngine(new VelocityTemplateEngine()) // 使用Velocity引擎
                .execute();
    }
}