package com.chr.fservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chr.fservice.entity.Book;
import com.chr.fservice.upload.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * 书籍表 服务类
 * </p>
 *
 * @author RAY
 * @since 2020-05-03
 */
public interface IBookService extends IService<Book> {
    String transfer(Runnable runnable);

    List<Book> getBookList(String dataSource);

    /**
     * 从ftp服务器下载任务
     * @param source
     * @param targetPrefix
     * @return
     */
    String uploadTask(String source, String targetPrefix, String batchNo) throws IOException;

    String uploadTask(String source, String targetPrefix) throws IOException;

    String uploadTask(InputStream inputStream, String sourceName, String target, long totalLength);

    /**
     * 上传任务至ftp服务器
     * @param source
     * @param target
     * @param isFtpOut
     * @return
     */
    String uploadTask(String source, String target, boolean isFtpOut) throws IOException;

    String addTask0(UploadTask task);
}
