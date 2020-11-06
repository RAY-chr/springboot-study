package com.chr.fweb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;


/**
 * @author RAY
 * @descriptions
 * @since 2020/5/13
 */
public class UploadFile implements Runnable {
    private Logger logger = LoggerFactory.getLogger(UploadFile.class);
    private MultipartFile multipartFile;
    private File file;

    public UploadFile(MultipartFile multipartFile, File file) {
        this.multipartFile = multipartFile;
        this.file = file;
    }

    @Override
    public void run() {
        try {
            logger.info("start transfer to file {}",file.getName());
            logger.info("current thread id : {}",Thread.currentThread().getId());
            multipartFile.transferTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
