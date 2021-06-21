package com.chr.fservice.entity;

/**
 * @author RAY
 * @descriptions
 * @since 2020/6/16
 */
public class Picture {
    private Integer id;
    private String path;
    private Integer version;

    public Picture(Integer id, String path) {
        this.id = id;
        this.path = path;
    }

    public Picture(Integer id, String path, Integer version) {
        this.id = id;
        this.path = path;
        this.version = version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Picture{");
        sb.append("id=").append(id);
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
