package com.chr.fservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author RAY
 * @since 2020-10-27
 */
public class Task extends Model<Task> {

    private static final long serialVersionUID = 1L;

    /**
     * uuid
     */
    private String id;

    /**
     * 文件源
     */
    private String source;

    /**
     * 批次号
     */
    @TableField("batchNo")
    private String batchNo;

    /**
     * 目标
     */
    private String target;

    /**
     * 总大小
     */
    @TableField("totalSize")
    private String totalSize;

    @TableField("startTime")
    private LocalDateTime startTime;

    @TableField("endTime")
    private LocalDateTime endTime;

    /**
     * 1为取消，0为正常
     */
    private String canceled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public String getCanceled() {
        return canceled;
    }

    public void setCanceled(String canceled) {
        this.canceled = canceled;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "Task{" +
            "id=" + id +
            ", source=" + source +
            ", batchNo=" + batchNo +
            ", target=" + target +
            ", totalSize=" + totalSize +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", canceled=" + canceled +
        "}";
    }
}
