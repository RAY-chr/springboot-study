package com.chr.fservice.config;

import com.chr.fservice.service.MasterDataSourceProperties;
import com.chr.fservice.service.SlaveDataSourceProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author RAY
 * @descriptions 动态数据源配置
 * @since 2020/9/16
 */
@Configuration
public class DataSourceConfig {

    @Autowired
    private MasterDataSourceProperties masterProperties;

    @Autowired
    private SlaveDataSourceProperties slaveProperties;

    @Bean(name = "master")
    public DataSource masterSource() {
        HikariConfig config = new HikariConfig();
//        dataSource.setJdbcUrl(masterProperties.getUrl());
//        dataSource.setUsername(masterProperties.getUsername());
//        dataSource.setPassword(masterProperties.getPassword());
//        dataSource.setDriverClassName(masterProperties.getDriver());
        config.setDataSource(new OptionalDataSource());
        config.setMinimumIdle(5);
        //config.setDataSourceClassName("com.chr.fservice.config.OptionalDataSource");
        /**
         * 使用有参构造器使用 fastPathPool 更快
         */
        return new HikariDataSource(config);
    }

    @Bean(name = "slave")
    public DataSource slaveSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(slaveProperties.getUrl());
        dataSource.setUsername(slaveProperties.getUsername());
        dataSource.setPassword(slaveProperties.getPassword());
        dataSource.setDriverClassName(slaveProperties.getDriver());
        return dataSource;
    }

    @Primary //表示使用自定义的数据源Key规则
    @Bean
    public RouteDataSource getSource() {
        RouteDataSource routeDataSource = new RouteDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterSource());
        targetDataSources.put("slave", slaveSource());
        routeDataSource.setTargetDataSources(targetDataSources);
        routeDataSource.setDefaultTargetDataSource(masterSource());
        return routeDataSource;
    }

    /**
     * 根据数据源的名称动态切换数据源
     */
    public static class RouteDataSource extends AbstractRoutingDataSource {

        @Override
        protected Object determineCurrentLookupKey() {
            /**
             * 相当于传了线程这个参数,哪个方法切换数据源，就设置自己想要的
             */
            return DataSourceContext.getDataSource();
        }
    }
}
