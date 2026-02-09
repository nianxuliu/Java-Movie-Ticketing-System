package com.movie.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    String value() default ""; // 操作描述，例如 "删除电影"
}