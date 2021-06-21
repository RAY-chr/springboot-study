package com.chr.fservice.service.async;

import com.chr.fservice.service.PictureConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author RAY
 * @descriptions
 * @since 2020/6/15
 */
@Service
public class AsyncTestService {

    @Autowired
    private PictureConfig pictureConfig;

    @Resource(name = "batchPool")
    private ThreadPoolExecutor batchPool;

    @PostConstruct  //@PostConstruct注解的方法在构造器执行后(依赖注入后),
    // applicationContext初始化完毕(手动获取bean)之前, init方法（servlet中）之前执行
    public void init() {
        System.out.println("===============================");
        System.out.println("初始化AsyncTestService。。。");
        System.out.println(pictureConfig.getCoreSize());
        System.out.println(batchPool);
        System.out.println("===============================");
    }

    @Async("batchPool")
    public Future<String> firstJob() {
        System.out.println(Thread.currentThread());
        System.out.println("start execute job1");
        return new AsyncResult<>("hello1");
    }

    @Async("batchPool")
    public Future<String> secondJob() {
        System.out.println(Thread.currentThread());
        System.out.println("start execute job2");
        return new AsyncResult<>("hello2");
    }

}
