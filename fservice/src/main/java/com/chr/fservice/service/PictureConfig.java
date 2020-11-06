package com.chr.fservice.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * @author RAY
 * @descriptions
 * @since 2020/6/17
 */
@Component
@ConfigurationProperties(prefix = "picture-config")
@Validated
public class PictureConfig {
    private String path;
    private String url;
    private Integer coreSize;

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public Integer getCoreSize() {
        return coreSize;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCoreSize(Integer coreSize) {
        this.coreSize = coreSize;
    }
}
