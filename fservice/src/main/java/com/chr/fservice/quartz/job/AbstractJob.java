package com.chr.fservice.quartz.job;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author RAY
 * @descriptions
 * @since 2021/3/19
 */
@DisallowConcurrentExecution
public abstract class AbstractJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(AbstractJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("======================== Job start =========================");
        logger.info("job class : {}", context.getJobDetail().getJobClass());
        Trigger trigger = context.getTrigger();
        if (trigger instanceof CronTrigger) {
            logger.info("job cron : {}", ((CronTrigger) trigger).getCronExpression());
        }
        this.execute();
        logger.info("======================== Job  end  =========================\n");
    }

    protected abstract void execute();
}
