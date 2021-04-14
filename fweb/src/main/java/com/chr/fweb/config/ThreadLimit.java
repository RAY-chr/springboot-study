package com.chr.fweb.config;

import java.lang.annotation.*;

/**
 * @author RAY
 * @descriptions
 * @since 2021/4/14
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThreadLimit {

    /**
     * 限定接口方法最多同时访问的线程数
     * @return
     */
    int size() default 10;
}
