package com.chr.fservice.quartz.job;

import com.chr.fservice.SpringUtil;
import com.chr.fservice.pool.CommonStoragePool;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author RAY
 * @descriptions
 * @since 2020/10/12
 */
public class HelloJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(HelloJob.class);
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        CommonStoragePool<String> commonPool = (CommonStoragePool<String>) SpringUtil.getBean("commonPool");
        CommonStoragePool<Integer> intPool = (CommonStoragePool<Integer>) SpringUtil.getBean("intPool");
        System.out.println(commonPool +" -> "+ commonPool.poll());
        System.out.println(intPool+" -> "+"int "+intPool.poll());
        logger.info("hello job");
    }

}
