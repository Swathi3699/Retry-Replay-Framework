package com.example.demo.controller;

import com.example.demo.model.Transaction;
import com.example.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {
    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/create")
    public ResponseEntity<List<Transaction>>createTransaction(@RequestBody List<Transaction> transaction) {
        List<Transaction> saved = transactionService.saveTransaction(transaction);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/get")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Transaction>> filterTransactions(
            @RequestParam String type,
            @RequestParam String status,
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        return ResponseEntity.ok(transactionService.filterTransactions(type, status, fromDate, toDate));
    }


    @PostMapping("/{id}/retry")
    public ResponseEntity<String> retryTransaction(@PathVariable Long id) {
        boolean result = transactionService.retryTransaction(id);
        if (result) {
            return ResponseEntity.ok("Retry successfully triggered");
        } else {
            return ResponseEntity.badRequest().body("Retry failed or not applicable");
        }
    }

    @PostMapping("/{id}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> replayTransaction(@PathVariable Long id) {
        boolean result = transactionService.replayTransaction(id);
        return result
                ? ResponseEntity.ok("Replay successful")
                : ResponseEntity.badRequest().body("Replay failed");
    }

}
