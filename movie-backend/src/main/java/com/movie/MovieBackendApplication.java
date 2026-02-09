package com.movie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.movie.mapper")
@EnableScheduling
public class MovieBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieBackendApplication.class, args);
	}

}
