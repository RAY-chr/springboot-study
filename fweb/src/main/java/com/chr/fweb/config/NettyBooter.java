package com.chr.fweb.config;


import com.chr.fservice.pool.CommonStoragePool;
import com.chr.fservice.quartz.QuartzJobHandle;
import com.chr.fservice.upload.UploadTask;
import com.chr.fweb.netty.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author RAY
 * @descriptions
 * @since 2020/3/29
 */
@Component
public class NettyBooter implements ApplicationRunner {
    @Autowired
    private HttpServer httpServer;

    @Autowired
    private QuartzJobHandle quartzJobHandle;

    @Autowired
    @Qualifier("uploadPool")
    private CommonStoragePool<UploadTask> queue;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(() -> {
            Thread.currentThread().setName("queue task");
            while (true) {
                try {
                    UploadTask task = queue.take();
                    System.out.println("start run " + task.getSource());
                    task.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.HelloJob",
                "1", "0/10 * * * * ?");
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.DataCompareJob",
                "1", "0/30 * * * * ?");
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.TestJob",
                "1", "0/5 * * * * ?");
        httpServer.start();
    }
}
