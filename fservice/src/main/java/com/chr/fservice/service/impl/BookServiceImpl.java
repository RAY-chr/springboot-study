package com.chr.fservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chr.fservice.config.DataSourceContext;
import com.chr.fservice.entity.Book;
import com.chr.fservice.mapper.BookMapper;
import com.chr.fservice.pool.CommonStoragePool;
import com.chr.fservice.pool.ThreadBatchPool;
import com.chr.fservice.service.IBookService;
import com.chr.fservice.upload.TaskDetailContainer;
import com.chr.fservice.upload.UploadTask;
import com.chr.fservice.upload.ftp.FTPUtil;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;

/**
 * <p>
 * 书籍表 服务实现类
 * </p>
 *
 * @author RAY
 * @since 2020-05-03
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {

    @Autowired
    private ThreadBatchPool batchPool;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private FTPUtil ftpUtil;

    @Autowired
    private CommonStoragePool<UploadTask> queue;

    @Bean("uploadPool")
    public CommonStoragePool<UploadTask> uploadPool() {
        return queue;
    }

    @Override
    public String transfer(Runnable runnable) {
        /*if (batchPool.queueIsFull()) {
            return "当前任务过多，请等待";
        }*/
        try {
            batchPool.getPool().submit(runnable);
        } catch (Exception e) {
            if (e instanceof RejectedExecutionException) {
                System.out.println(e);
                return "当前任务过多，请等待";
            }
        }
        return "success";
    }

    @Override
    public List<Book> getBookList(String dataSource) {
        DataSourceContext.setDataSource(dataSource);
        List<Book> books = bookMapper.test();
        DataSourceContext.clearCache();
        return books;
    }

    /**
     * 上传任务
     *
     * @param source
     * @param targetprefix
     * @param batchNo
     * @return
     */
    @Override
    public String uploadTask(String source, String targetprefix, String batchNo) throws IOException {
        FTPClient ftpClient = ftpUtil.getFtpClient();
        List<String> files = ftpUtil.getAbsolutePathFiles(source, ftpClient);
        for (String file : files) {
            this.addTask(file, targetprefix, batchNo);
        }
        ftpClient.disconnect();
        return "success";
    }

    @Override
    public String uploadTask(String source, String targetprefix) throws IOException {
        return uploadTask(source, targetprefix, null);
    }

    @Override
    public String uploadTask(InputStream inputStream, String sourceName, String target, long totalLength) {
        UploadTask task = new UploadTask(inputStream, sourceName, target, totalLength);
        return this.addTask0(task);
    }

    @Override
    public String uploadTask(String source, String target, boolean isFtpOut) throws IOException {
        FTPClient ftpClient = ftpUtil.getFtpClient();
        long size = ftpUtil.getSize(target, ftpClient);
        if (size > 0) {
            ftpClient.disconnect();
            return "already exisit this target";
        }
        ftpUtil.mkdirs(target.substring(0, target.lastIndexOf("/")), ftpClient);
        ftpClient.disconnect();
        UploadTask task = new UploadTask(source, target, true);
        return this.addTask0(task);
    }

    public String addTask(String source, String targetprefix, String batchNo) {
        String target = targetprefix + System.currentTimeMillis() + getRandNum(6)
                + source.substring(source.lastIndexOf("/") + 1);
        UploadTask task = new UploadTask(source, target, batchNo);
        return this.addTask0(task);
    }

    /**
     * 添加任务到线程池，如果添加失败，则加入任务至消费队列里面
     *
     * @param task
     * @return
     */
    public String addTask0(UploadTask task) {
        if (task.getPausedPosition() == 0) {
            TaskDetailContainer.add(task);
        }
        if (!this.transfer(task).equals("success")) {
            try {
                queue.put(task);
                return "success";
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return "success";
    }

    public static String getRandNum(int charCount) {
        String charValue = "";
        for (int i = 0; i < charCount; i++) {
            char c = (char) (randomInt(0, 10) + '0');
            charValue += String.valueOf(c);
        }
        return charValue;
    }

    public static int randomInt(int from, int to) {
        Random r = new Random();
        return from + r.nextInt(to - from);
    }

}
