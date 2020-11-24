package com.chr.fweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author RAY
 * @descriptions 多模块SpringBoot项目
 * @since 2020/5/3
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) //禁用springboot默认加载数据源配置
@ComponentScan(basePackages = {"com.chr.fservice","com.chr.fweb"})
@MapperScan("com.chr.fservice.mapper")
@EnableAsync
public class FwebApplication {

    public static void main(String[] args) {
        SpringApplication.run(FwebApplication.class, args);
    }
}
