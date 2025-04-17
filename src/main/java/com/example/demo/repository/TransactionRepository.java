package com.example.demo.repository;

import com.example.demo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    List<Transaction>findByStatus(String status);

    List<Transaction> findByTypeAndStatus(String type, String status);

    Transaction findByTransactionId(String transactionId);
}
