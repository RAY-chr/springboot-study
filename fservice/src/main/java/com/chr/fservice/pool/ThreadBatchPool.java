package com.chr.fservice.pool;


import com.chr.fservice.SpringUtil;
import com.chr.fservice.service.PictureConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author RAY
 * @descriptions
 * @since 2020/5/13
 */
@Component
public class ThreadBatchPool {

    @Autowired
    private PictureConfig pictureConfig;
    private int CORSIZE;
    @Value("${pool.maxSize}")
    private int MAXSIZE;
    @Value(("${pool.queue.size}"))
    private int queueSize;
    private ArrayBlockingQueue<Runnable> queue;
    private ThreadPoolExecutor pool;
    private AtomicBoolean newPool = new AtomicBoolean(false);

    @Bean("batchPool")
    public ThreadPoolExecutor getPool() {
        //setCOR();
        if (pool == null) {
            CORSIZE = pictureConfig.getCoreSize();
            queue = new ArrayBlockingQueue<>(queueSize);
            ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("batch_thread-%d").build();
            //这里项目启动的时候，Spring就会初始化pool（加入了Bean），否则需要考虑多线程初始化pool的问题
            if (newPool.compareAndSet(false, true)) {
                pool = new ThreadPoolExecutor(CORSIZE,
                        MAXSIZE, 20, TimeUnit.SECONDS, queue, factory);
            }
        }
        return pool;
    }

    public boolean queueIsFull() {
        return queue.size() >= 3;
    }

    /**
     * 在本身加入了spring容器中的类的构造器中无法获取到依赖项（即使手动获取）,所以下面语句会报错
     * 但是静态方法却可以手动获取
     * <p>
     * spring在创建beanFactory实例化类的时候，先加载类，然后调用类的构造器进行初始化
     * 在完成初始化后进行依赖注入（常见的就是set注入，通过执行set方法）
     */
    public ThreadBatchPool() {
        //CORSIZE = pictureConfig.getCoreSize();
        //CORSIZE = SpringUtil.getBean(PictureConfig.class).getCoreSize();
    }

    public void setCOR() {
        this.CORSIZE = SpringUtil.getBean(PictureConfig.class).getCoreSize();
    }

}
