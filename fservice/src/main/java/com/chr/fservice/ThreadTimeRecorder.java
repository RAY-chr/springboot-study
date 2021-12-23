package com.chr.fservice;

import java.util.concurrent.TimeUnit;

/**
 * @author RAY
 * @descriptions
 * @since 2021/12/23
 */
public class ThreadTimeRecorder {

    private static final ThreadLocal<TimeRecorder> recorders = new ThreadLocal<>();

    /**
     * 默认创建毫秒单位的记录器
     */
    public static void create() {
        create(TimeUnit.MILLISECONDS);
    }

    /**
     * 创建当前线程的阶段时间记录器
     *
     * @param unit 时间单位
     */
    public static void create(TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (unit == TimeUnit.MILLISECONDS) {
            recorders.set(new MillisTimeRecorder());
        } else if (unit == TimeUnit.NANOSECONDS) {
            recorders.set(new NanoTimeRecorder());
        } else {
            throw new IllegalArgumentException("can't find the type");
        }
    }

    /**
     * 记录当前阶段消耗的时间
     *
     * @param stageName 阶段名
     */
    public static void record(String stageName) {
        TimeRecorder recorder = recorders.get();
        if (recorder == null) {
            throw new IllegalStateException("ThreadTimeRecorder not create TimeRecorder");
        }
        recorder.record(stageName);
    }

    /**
     * 记录流程结束后，清除当前线程的记录器
     */
    public static void destroy() {
        if (recorders.get() != null) {
            recorders.remove();
        }
    }

    /**
     * 纳秒单位记录器
     */
    public static class NanoTimeRecorder implements TimeRecorder {

        private long start;

        public NanoTimeRecorder() {
            this.start = System.nanoTime();
        }

        @Override
        public void record(String stageName) {
            long end = System.nanoTime();
            System.err.println(Thread.currentThread().getName() + " ==> " + stageName + " ==> " +
                    (end - start) + " ns");
            start = end;
        }

    }

    /**
     * 毫秒单位记录器
     */
    public static class MillisTimeRecorder implements TimeRecorder {

        private long start;

        public MillisTimeRecorder() {
            this.start = System.currentTimeMillis();
        }

        @Override
        public void record(String stageName) {
            long end = System.currentTimeMillis();
            System.err.println(Thread.currentThread().getName() + " ==> " + stageName + " ==> " +
                    (end - start) + " ms");
            start = end;
        }

    }

    public interface TimeRecorder {

        void record(String stageName);

    }

}
