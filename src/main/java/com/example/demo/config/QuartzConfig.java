package com.example.demo.config;

import com.example.demo.job.RetryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail retryJobDetail() {
        return JobBuilder.newJob(RetryJob.class)
                .withIdentity("retryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger retryJobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(5) // Change to desired interval
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(retryJobDetail())
                .withIdentity("retryTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
