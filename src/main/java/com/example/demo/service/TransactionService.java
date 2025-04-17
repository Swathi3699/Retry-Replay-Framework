package com.example.demo.service;

import com.example.demo.model.Transaction;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    List<Transaction> saveTransaction(List<Transaction> transaction);

    List<Transaction> getAllTransactions();

    List<Transaction> getTransactionsByTypeAndStatus(String type, String status);

    Transaction getTransactionById(Long id);

    Transaction getTransactionByTransactionId(String transactionId);

    void triggerRetry(String transactionId);

    List<Transaction> filterTransactions(String type, String status, LocalDateTime fromDate, LocalDateTime toDate);

    boolean retryTransaction(Long id);

    void retryFailedTransactions();

    boolean replayTransaction(Long id);


}
