package com.chr.fservice.upload;

import com.chr.fservice.SpringUtil;
import com.chr.fservice.service.IBookService;

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
    private static final List<UploadTask> list = new ArrayList<>();

    public static synchronized void add(UploadTask uploadTask) {
        list.add(uploadTask);
    }

    /**
     * 拿到当前传输任务的信息
     *
     * @return
     */
    public static synchronized List<UploadTask> get() {
        List<UploadTask> tasks;
        tasks = list.stream().filter(x -> x.getPercent().equals("100.00%") || x.isCanceled())
                .collect(Collectors.toList());
        list.removeAll(tasks);
        return list;
    }

    /**
     * 取消某个传输任务
     *
     * @param source
     */
    public static void cancel(String source) {
        List<UploadTask> tasks = getList(x -> x.getSource().equalsIgnoreCase(source)
                && !x.getPercent().equals("100.00%"));
        if (tasks.size() >= 1) {
            tasks.get(0).setCanceled(true);
        }
    }

    /**
     * 暂停某个传输任务
     *
     * @param source
     */
    public static void pause(String source) {
        List<UploadTask> tasks = getList(x -> x.getSource().equalsIgnoreCase(source)
                && !x.getPercent().equals("100.00%"));
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
        List<UploadTask> tasks = getList(x -> x.getSource().equalsIgnoreCase(source)
                && !x.getPercent().equals("100.00%") && x.isPaused());
        if (tasks.size() >= 1) {
            UploadTask task = tasks.get(0);
            task.setPaused(false);
            IBookService bookService = SpringUtil.getBean(IBookService.class);
            bookService.addTask0(task);
        }
    }

    public static List<UploadTask> getList(Predicate<? super UploadTask> predicate) {
        List<UploadTask> tasks = list.stream().filter(predicate)
                .collect(Collectors.toList());
        return tasks;
    }
}
