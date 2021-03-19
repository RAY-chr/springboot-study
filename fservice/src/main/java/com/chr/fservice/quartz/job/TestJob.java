package com.chr.fservice.quartz.job;

/**
 * @author RAY
 * @descriptions
 * @since 2021/3/19
 */
public class TestJob extends AbstractJob {
    @Override
    protected void execute() {
        System.out.println("Test --------->>>>>>>");
    }
}
