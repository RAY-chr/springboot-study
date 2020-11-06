package com.chr.fservice.config;

/**
 * @author RAY
 * @descriptions 不同线程数据源的上下文
 * @since 2020/9/16
 */
public class DataSourceContext {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setDataSource(String sourceName) {
        contextHolder.set(sourceName);
    }

    public static String getDataSource() {
        return contextHolder.get();
    }

    public static void clearCache() {
        contextHolder.remove();
    }

}
