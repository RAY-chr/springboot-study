package com.chr.fweb.config;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chr.fservice.entity.Task;
import com.chr.fservice.pool.CommonStoragePool;
import com.chr.fservice.quartz.QuartzJobHandle;
import com.chr.fservice.service.ITaskService;
import com.chr.fservice.upload.TaskDetailContainer;
import com.chr.fservice.upload.UploadTask;
import com.chr.fweb.netty.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

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
    private ITaskService taskService;

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
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.HelloJob", null,
                "1", "0/10 * * * * ?");
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.DataCompareJob", null,
                "1", "0/30 * * * * ?");
        quartzJobHandle.addJob("com.chr.fservice.quartz.job.TestJob", null,
                "1", "0/5 * * * * ?");
        this.loadDbTasksToMemory();
        httpServer.start();
    }

    private void loadDbTasksToMemory() {
        List<Task> tasks = taskService.list(new LambdaQueryWrapper<Task>().eq(Task::getPaused, "1")
                .or().isNull(Task::getPaused));
        for (Task task : tasks) {
            String target = task.getTarget();
            File file = new File(target);
            if (!file.exists() || file.length() == 0) {
                task.setCanceled("1");
                task.setPaused("0");
                taskService.updateById(task);
            } else {
                UploadTask uploadTask = new UploadTask(task.getSource(), target, task.getBatchNo());
                uploadTask.setId(task.getId());
                uploadTask.setPaused(true);
                uploadTask.setTotalLength(Long.parseLong(task.getTotalSize()));
                uploadTask.setPausedPosition(file.length());
                TaskDetailContainer.add(uploadTask);
            }
        }
    }
}
