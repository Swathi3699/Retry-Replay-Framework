package com.example.demo.job;

import com.example.demo.service.TransactionService;
import org.quartz.JobExecutionContext;
import org.quartz.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component


public class RetryJob implements Job {
    @Autowired
    private TransactionService transactionService;

    private static final Logger log = LoggerFactory.getLogger(RetryJob.class);


    @Override
    public void execute(JobExecutionContext context) {
        log.info("RetryJob triggered. Starting retry of failed transactions...");
        transactionService.retryFailedTransactions();
        log.info("RetryJob finished.");
    }
}
