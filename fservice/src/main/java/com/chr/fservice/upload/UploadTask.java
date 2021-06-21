package com.chr.fservice.upload;

import com.baomidou.mybatisplus.generator.config.IFileCreate;
import com.chr.fservice.SpringUtil;
import com.chr.fservice.entity.Task;
import com.chr.fservice.quartz.job.HelloJob;
import com.chr.fservice.service.ITaskService;
import com.chr.fservice.upload.ftp.FTPUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.SocketInputStream;
import org.apache.commons.net.io.SocketOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author RAY
 * @descriptions
 * @since 2020/10/26
 */
public class UploadTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(UploadTask.class);
    private String id;
    private String batchNo = "1";
    private String source = "";
    private InputStream inputStream;
    private String target;
    private volatile long currLength;
    private volatile long totalLength;
    private volatile String percent = "0%";
    private volatile boolean canceled = false;
    private volatile boolean paused = false;
    private boolean isFtpOut = false;
    private volatile long pausedPosition = 0;
    private ITaskService taskService;

    public UploadTask(String source, String target, boolean isFtpOut) {
        this.source = source;
        this.target = target;
        this.isFtpOut = isFtpOut;
    }

    public UploadTask(InputStream inputStream, String source, String target, long totalLength) {
        this.inputStream = inputStream;
        this.source = source;
        this.target = target;
        this.totalLength = totalLength;
    }

    public UploadTask(String source, String target, String batchNo) {
        this.source = source;
        this.target = target;
        this.batchNo = (batchNo == null || batchNo.length() == 0) ? "1" : batchNo;
    }

    @Override
    public void run() {
        taskService = SpringUtil.getBean(ITaskService.class);
        FTPUtil ftpUtil = SpringUtil.getBean(FTPUtil.class);
        InputStream in = null;
        OutputStream out = null;
        FTPClient ftpClient = null;
        byte[] buffer = new byte[4096];
        int read = 0;
        try {
            if (!isFtpOut && this.inputStream == null) {
                ftpClient = ftpUtil.getFtpClient();
                totalLength = ftpUtil.getSize(source, ftpClient);
                InputStream remoteInputStream = ftpClient.retrieveFileStream(source);
                in = remoteInputStream;
            } else {
                in = this.inputStream;
            }
            if (isFtpOut) {
                File file = new File(source);
                totalLength = file.length();
                in = new FileInputStream(file);
                ftpClient = ftpUtil.getFtpClient();
                // 这句话表明设置为二进制文件 不加的话上传至ftp服务器的文件会变大
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                out = ftpClient.appendFileStream(target);
            } else {
                out = new FileOutputStream(target, true);
            }
            int lastTime = (int) (totalLength / 4096), count = 0;
            LocalDateTime start = LocalDateTime.now();
            currLength = 0;
            while (count <= lastTime + 1 && (read = in.read(buffer)) != -1) {
                if (canceled) {
                    logger.warn("[{}] canceled = {}", source, canceled);
                    Task id = taskService.getById(this.id);
                    if (id != null) {
                        id.setTotalSize(String.valueOf(totalLength));
                        id.setCanceled("1");
                        id.setPaused("0");
                        taskService.updateById(id);
                    }
                    break;
                }
                // 保证读取的字节为字节数组长度
                while (count < lastTime && read < 4096) {
                    read += in.read(buffer, read, 4096 - read);
                }
                count++;
                currLength = currLength + read;
                if (pausedPosition > 0) {
                    if (currLength <= pausedPosition) {
                        continue;
                    }
                }
                out.write(buffer, 0, read);
                if (paused && currLength < totalLength) {
                    pausedPosition = currLength;
                    logger.warn("[{}] paused = {}, pausedPosition = {}", source, paused, pausedPosition);
                    Task id = taskService.getById(this.id);
                    if (id != null) {
                        id.setTotalSize(String.valueOf(totalLength));
                        id.setCanceled("0");
                        id.setPaused("1");
                        taskService.updateById(id);
                    }
                    break;
                }
                percent = this.count(currLength, totalLength);
            }
            if (!canceled && !paused) {
                this.saveTask(start);
            }
        } catch (Exception e) {
            e.printStackTrace();
            canceled = true;
            if (e instanceof IOException) {
                pausedPosition = currLength - read;
                logger.warn("[{}] caught exception, canceled = {}, pausedPosition = {}", source, canceled, pausedPosition);
            }
        } finally {
            try {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
                if (ftpClient != null) {
                    ftpClient.completePendingCommand();
                    ftpClient.disconnect();
                }
                if (canceled) {
                    File file = new File(target);
                    if (file != null) {
                        file.delete();
                        logger.info("delete the file [{}]", target);
                    }
                }
                buffer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void saveTask(LocalDateTime start) {
        LocalDateTime end = LocalDateTime.now();
        Task task = taskService.getById(this.id);
        if (task != null) {
            task.setStartTime(start);
            task.setEndTime(end);
            task.setTotalSize(totalLength + "");
            task.setCanceled("0");
            task.setPaused("0");
            taskService.updateById(task);
        }
        logger.info("[{}] upload success, totalSize [{}]", source, totalLength);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public long getCurrLength() {
        return currLength;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public String getPercent() {
        return percent;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCurrLength(int currLength) {
        this.currLength = currLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public long getPausedPosition() {
        return pausedPosition;
    }

    public void setPausedPosition(long pausedPosition) {
        this.pausedPosition = pausedPosition;
    }

    public String count(long first, long other) {
        Long x = first, y = other;
        String result = String.format("%.2f", ((x.doubleValue() / y.doubleValue()) * 100)) + "%";
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UploadTask{");
        sb.append("batchNo='").append(batchNo).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", currLength=").append(currLength);
        sb.append(", totalLength=").append(totalLength);
        sb.append(", percent='").append(percent).append('\'');
        sb.append(", canceled=").append(canceled);
        sb.append('}');
        return sb.toString();
    }
}
