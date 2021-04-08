package com.chr.fservice.upload;

import com.chr.fservice.SpringUtil;
import com.chr.fservice.service.IBookService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author RAY
 * @descriptions
 * @since 2020/10/26
 */
public class TaskDetailContainer {
    private static final List<UploadTask> TASKS = new ArrayList<>();

    public static synchronized void add(UploadTask uploadTask) {
        TASKS.add(uploadTask);
    }

    /**
     * 拿到当前传输任务的信息
     *
     * @return
     */
    public static List<UploadTask> get() {
        List<UploadTask> tasks = TASKS.stream().filter(x -> equals100(x) || x.isCanceled())
                .collect(Collectors.toList());
        synchronized (TaskDetailContainer.class) {
            TASKS.removeAll(tasks);
        }
        return TASKS;
    }

    /**
     * 取消某个传输任务
     *
     * @param source
     */
    public static void cancel(String source) {
        List<UploadTask> tasks = predicateList(x -> selectOne(x, source));
        if (tasks.size() >= 1) {
            UploadTask task = tasks.get(0);
            // 任务暂停了再取消，直接删除本地文件即可
            if (task.isPaused()) {
                task.setCanceled(true);
                String target = task.getTarget();
                File file = new File(target);
                if (file != null) {
                    file.delete();
                    System.out.println("delete the file [" + target + "]");
                }
            } else {
                task.setCanceled(true);
            }
        }
    }

    /**
     * 暂停某个传输任务
     *
     * @param source
     */
    public static void pause(String source) {
        List<UploadTask> tasks = predicateList(x -> selectOne(x, source));
        if (tasks.size() >= 1) {
            tasks.get(0).setPaused(true);
        }
    }

    /**
     * 恢复某个传输任务
     *
     * @param source
     */
    public static void resume(String source) {
        List<UploadTask> tasks = predicateList(x -> selectOne(x, source) && x.isPaused());
        if (tasks.size() >= 1) {
            UploadTask task = tasks.get(0);
            task.setPaused(false);
            IBookService bookService = SpringUtil.getBean(IBookService.class);
            bookService.addTask0(task);
        }
    }

    /**
     * 从任务管理器中选择正在运行的任务
     *
     * @param x
     * @param source
     * @return
     */
    private static boolean selectOne(UploadTask x, String source) {
        return x.getSource().equalsIgnoreCase(source) && !equals100(x);
    }

    /**
     * 任务的进度是否为100%
     *
     * @param x
     * @return
     */
    private static boolean equals100(UploadTask x) {
        return x.getPercent().equals("100.00%");
    }

    public static List<UploadTask> predicateList(Predicate<? super UploadTask> predicate) {
        return TASKS.stream().filter(predicate).collect(Collectors.toList());
    }

}
