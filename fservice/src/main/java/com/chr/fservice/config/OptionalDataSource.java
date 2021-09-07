package com.chr.fservice.config;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @author RAY
 * @descriptions 测试 HikariDataSource 设置自定义数据源获取连接
 * @since 2021/7/21
 */
public class OptionalDataSource implements DataSource {
    private static final String defaultUrl = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&" +
            "characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String defaultDriver = "com.mysql.jdbc.Driver";
    private static final String username = "root";
    private static final String password = "root";

    private String driver;
    private String url;

    public OptionalDataSource() {
        this.driver = defaultDriver;
        this.url = defaultUrl;
    }

    public OptionalDataSource(String driver, String url) {
        this.driver = driver;
        this.url = url;
    }


    @Override
    public Connection getConnection() throws SQLException {
        System.err.println(">>>>>>===================<<<<<<<");
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
