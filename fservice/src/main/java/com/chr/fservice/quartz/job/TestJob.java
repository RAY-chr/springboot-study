package com.chr.fservice.quartz.job;

import org.quartz.JobExecutionContext;

/**
 * @author RAY
 * @descriptions
 * @since 2021/3/19
 */
public class TestJob extends AbstractJob {
    @Override
    protected void doExecute(JobExecutionContext context) {
        System.out.println("Test --------->>>>>>>");
    }
}
