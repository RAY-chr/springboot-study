package com.chr.fservice.quartz;

import com.chr.fservice.mapper.BookMapper;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author RAY
 * @descriptions
 * @since 2020/10/12
 */
@Component
public class QuartzJobHandle {
    private static Logger logger = LoggerFactory.getLogger(QuartzJobHandle.class);
    @Autowired
    @Qualifier("scheduler")
    private Scheduler scheduler;

    @Autowired
    private BookMapper bookMapper;

    /**
     * 添加job
     *
     * @param jobClassName
     * @param jobGroupName
     * @param cronExpression
     * @throws Exception
     */
    public void addJob(String jobClassName, String jobGroupName, String cronExpression) throws Exception {
        // 启动调度器
        scheduler.start();
        //构建job信息
        JobDetail jobDetail = JobBuilder.newJob(
                ((Job) Class.forName(jobClassName).newInstance()).getClass())
                .withIdentity(jobClassName, jobGroupName).build();
        //表达式调度构建器(即任务执行的时间)
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        //按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobClassName, jobGroupName)
                .withSchedule(scheduleBuilder).build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("添加任务 {} 成功", jobDetail.getKey());
        } catch (Exception e) {
            System.out.println("创建定时任务失败" + e);
        }
    }

    /**
     * 更新job
     *
     * @param jobClassName
     * @param jobGroupName
     * @param cronExpression
     * @throws Exception
     */
    public void rescheduleJob(String jobClassName, String jobGroupName, String cronExpression) throws Exception {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobClassName, jobGroupName);
            // 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            // 按新的cronExpression表达式重新构建trigger
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            // 按新的trigger重新设置job执行
            scheduler.rescheduleJob(triggerKey, trigger);
            logger.info("更新任务 {} 成功", triggerKey);
        } catch (SchedulerException e) {
            System.out.println("更新定时任务失败"+e);
        }
    }

    /**
     * 暂停job
     *
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    public void pauseJob(String jobClassName, String jobGroupName) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobClassName, jobGroupName);
        scheduler.pauseJob(jobKey);
        logger.info("暂停任务 {} 成功", jobKey);
    }

    /**
     * 恢复job
     *
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    public void resumeJob(String jobClassName, String jobGroupName) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobClassName, jobGroupName);
        scheduler.resumeJob(jobKey);
        logger.info("恢复任务 {} 成功", jobKey);
    }

    public List<JobContent> listAllJobs() {
        return bookMapper.listAllJobs();
    }



}
