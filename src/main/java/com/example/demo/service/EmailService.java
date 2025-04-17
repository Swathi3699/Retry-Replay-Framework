package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendRetryFailureEmail(String transactionId, String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Retry Failed for Transaction " + transactionId);
        message.setText("Transaction " + transactionId + " failed after all retry attempts.");

        mailSender.send(message);
    }

    public void sendReplayFailureEmail(String transactionId, String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(" Replay Failed for Transaction " + transactionId);
        message.setText("Transaction " + transactionId + " failed during manual replay attempt.");

        mailSender.send(message);
    }
}
