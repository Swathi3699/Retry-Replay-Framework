package com.example.demo.service;

import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class TransactionServiceImp implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionServiceImp(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Autowired
    private RetryTemplate fixedRetryTemplate;

    @Autowired
    private RetryTemplate exponentialRetryTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private EmailService emailService;



    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);


    @Override
    public List<Transaction> saveTransaction(List<Transaction> transaction) {
       return transactionRepository.saveAll(transaction);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public List<Transaction> getTransactionsByTypeAndStatus(String type, String status) {
        return transactionRepository.findByTypeAndStatus(type,status);
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    @Override
    public Transaction getTransactionByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    @Override
    public void triggerRetry(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId);
        if (transaction != null && "FAILED".equalsIgnoreCase(transaction.getStatus())) {
            // Simulate retry
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
        }
    }

    @Override
    public List<Transaction> filterTransactions(String type, String status, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Transaction> allMatches = transactionRepository.findByTypeAndStatus(type, status);
        return allMatches.stream()
                .filter(tx -> tx.getLastAttempt() != null &&
                        (tx.getLastAttempt().isAfter(fromDate) || tx.getLastAttempt().isEqual(fromDate)) &&
                        (tx.getLastAttempt().isBefore(toDate) || tx.getLastAttempt().isEqual(toDate)))
                .toList();
    }



    @Override
    public void retryFailedTransactions() {
        List<Transaction> failedTransactions = transactionRepository.findByStatus("FAILED");

        log.info("Found {} failed transactions to retry", failedTransactions.size());

        for (Transaction txn : failedTransactions) {
            try {
                // Custom logic to retry transaction
                boolean success = processTransaction(txn);
                txn.setStatus(success ? "SUCCESS" : "FAILED");
                transactionRepository.save(txn);
            } catch (Exception e) {
                log.error("Retry failed for transaction id {}: {}", txn.getId(), e.getMessage());
            }
        }
    }

    @Override
    public boolean replayTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id).orElse(null);
        if (tx == null) return false;

        try {

            tx.setStatus("REPLAYED");
            tx.setLastAttempt(LocalDateTime.now());
            tx.setRetryCount(tx.getRetryCount() + 1);

            transactionRepository.save(tx);
            log.info("Transaction {} replayed by admin", tx.getTransactionId());

            return true;
        } catch (Exception e) {

            log.error("Replay failed for transaction {}: {}", id, e.getMessage());
            emailService.sendReplayFailureEmail(tx.getTransactionId(), "admin@example.com");
            return false;
        }
    }

    private boolean processTransaction(Transaction txn) {

        log.info("Retrying transaction ID: {}, type: {}",
                txn.getId(), txn.getType());

        try {

            boolean result = externalServiceCall(txn);

            if (result) {
                log.info("Transaction {} processed successfully.", txn.getId());
                return true;
            } else {
                log.warn("Transaction {} failed to process.", txn.getId());
                return false;
            }

        } catch (Exception e) {
            log.error("Error while processing transaction {}: {}", txn.getId(), e.getMessage());
            return false;
        }
    }

    private boolean externalServiceCall(Transaction txn) throws InterruptedException {

        Thread.sleep(500);


        return Math.random() < 0.8;
    }

    @Override
    public boolean retryTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id).orElse(null);
        if (tx == null || !"FAILED".equalsIgnoreCase(tx.getStatus())) {
            return false;
        }

        String strategy = tx.getRetryStrategy() != null ? tx.getRetryStrategy().toUpperCase() : "FIXED";

        switch (strategy) {
            case "EXPONENTIAL":
                return retryWithTemplate(exponentialRetryTemplate, tx);

            case "FIXED":
                return retryWithTemplate(fixedRetryTemplate, tx);

            case "JITTER":
                return retryWithJitter(tx);

            case "CIRCUIT_BREAKER":
                return retryWithCircuitBreaker(tx);

            default:
                log.warn("Unknown retry strategy '{}', using default FIXED strategy.", strategy);
                return retryWithTemplate(fixedRetryTemplate, tx);
        }
    }

    private boolean retryWithTemplate(RetryTemplate retryTemplate, Transaction tx) {

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        return retryTemplate.execute(ctx -> {
            log.info("Retrying transaction {} using {} strategy (attempt {})",
                    tx.getTransactionId(), tx.getRetryStrategy(), ctx.getRetryCount(),correlationId);


            if (Math.random() < 0.5) {
                throw new RuntimeException("Simulated processing failure");
            }

            tx.setStatus("SUCCESS");
            tx.setRetryCount(tx.getRetryCount() + 1);
            tx.setLastAttempt(LocalDateTime.now());
            transactionRepository.save(tx);
            MDC.clear();
            return true;
        }, context -> {
            log.error("All retry attempts failed for transaction {}", tx.getTransactionId(),correlationId);
            tx.setRetryCount(tx.getRetryCount() + 1);
            tx.setLastAttempt(LocalDateTime.now());
            transactionRepository.save(tx);
            MDC.clear();
            emailService.sendRetryFailureEmail(tx.getTransactionId(), "admin@example.com");

            return false;
        });
    }

    private boolean retryWithJitter(Transaction tx) {
        int maxAttempts = 5;
        Random random = new Random();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                log.info("Jitter retry attempt {} for transaction {}", attempt + 1, tx.getTransactionId());


                if (Math.random() < 0.5) {
                    throw new RuntimeException("Simulated failure");
                }

                tx.setStatus("SUCCESS");
                tx.setRetryCount(tx.getRetryCount() + 1);
                tx.setLastAttempt(LocalDateTime.now());
                transactionRepository.save(tx);

                log.info("Transaction {} succeeded on attempt {}", tx.getTransactionId(), attempt + 1);
                return true;

            } catch (Exception e) {
                long baseDelay = (long) Math.pow(2, attempt) * 1000;
                long jitter = random.nextInt(1000); // Random 0–999 ms
                long delay = baseDelay + jitter;

                log.warn("Retry attempt {} failed. Waiting {} ms before next retry.", attempt + 1, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }


        tx.setRetryCount(tx.getRetryCount() + 1);
        tx.setLastAttempt(LocalDateTime.now());
        transactionRepository.save(tx);

        log.error("All jitter retries failed for transaction {}", tx.getTransactionId());
        return false;
    }

    private boolean retryWithCircuitBreaker(Transaction tx) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("retryService");

        Supplier<Boolean> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            log.info("CircuitBreaker retry for transaction {}", tx.getTransactionId());


            if (Math.random() < 0.5) {
                throw new RuntimeException("Simulated circuit failure");
            }

            tx.setStatus("SUCCESS");
            tx.setRetryCount(tx.getRetryCount() + 1);
            tx.setLastAttempt(LocalDateTime.now());
            transactionRepository.save(tx);
            return true;
        });

        try {
            return decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.error("Circuit breaker is OPEN — retry skipped for transaction {}", tx.getTransactionId());
            return false;
        } catch (Exception e) {
            log.error("Retry via circuit breaker failed: {}", e.getMessage());
            tx.setRetryCount(tx.getRetryCount() + 1);
            tx.setLastAttempt(LocalDateTime.now());
            transactionRepository.save(tx);
            return false;
        }
    }



}
