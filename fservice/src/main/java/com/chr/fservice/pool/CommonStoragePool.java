package com.chr.fservice.pool;

import com.chr.fservice.quartz.job.HelloJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author RAY
 * @descriptions
 * @since 2020/10/21
 */
@Component
@Scope("prototype")
public class CommonStoragePool<T> {
    private static final int DEFAULT_SIZE = 20;
    private Logger logger = LoggerFactory.getLogger(CommonStoragePool.class);
    private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(DEFAULT_SIZE);

    public void setOwnSize(int size) {
        queue = new ArrayBlockingQueue<>(size);
    }

    public boolean add(T t) {
        boolean b = false;
        try {
            b = queue.add(t);
        } catch (Exception e) {
            logger.warn("put >>>{}<<< fail, because the pool is full", t);
        }
        return b;
    }

    public void put(T t) throws InterruptedException {
        queue.put(t);
    }

    public T poll() {
        Object poll = queue.poll();
        if (poll == null) {
            logger.warn("the pool is empty");
        }
        return (T) poll;
    }

    public T take() throws InterruptedException {
        Object take = queue.take();
        return (T) take;
    }

}
