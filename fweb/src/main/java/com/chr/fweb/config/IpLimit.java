package com.chr.fweb.config;

import java.lang.annotation.*;

/**
 * @author RAY
 * @descriptions
 * @since 2021/4/8
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IpLimit {

    /**
     * 限定时间的次数
     * @return
     */
    int times() default 10;

    /**
     * 限定的时间
     * @return
     */
    long expire() default 3600L;
}
