#  Retry & Replay Framework (Spring Boot)

A robust, extensible framework built using Spring Boot to automatically retry failed transactions and manually replay them, with support for various retry strategies, logging, monitoring, and notifications — built for real-world distributed systems.

---

##  Features

- **Quartz-based Scheduler** to retry failed transactions automatically
- **Manual Retry API** for specific transactions
-  **Manual Replay API** for reprocessing any transaction
-  **Pluggable Retry Strategies**:
  - Fixed Interval
  - Exponential Backoff
  - Circuit Breaker (Resilience4j)
  - Jitter (randomized exponential delay)
-  **Role-based access control** (Spring Security)
- **Email Notifications** on retry/replay failures
- **Structured Logging with Correlation ID**
-  **Postman-ready APIs** for demo and testing

---

## Tech Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| Backend      | Java 17, Spring Boot              |
| Scheduling   | Quartz                            |
| Retry Engine | Spring Retry, RetryTemplate       |
| Circuit Breaker | Resilience4j                   |
| Security     | Spring Security with Roles        |
| Mail         | Spring Boot Mail (SMTP/Gmail)     |
| Logging      | SLF4J, Logback, Correlation ID    |

---

##  Project Modules

- `Transaction` Entity with retry metadata (strategy, count, last attempt)
- `RetryJob` (Quartz) to auto-trigger retries
- `TransactionService` containing all retry strategy logic
- `EmailService` for alerts
- `SecurityConfig` for admin-restricted replay access
- `logback-spring.xml` for structured log output

---

##  API Endpoints

| Method | Endpoint                          | Description                             |
|--------|-----------------------------------|-----------------------------------------|
| `POST` | `/transactions/create`            | Add new transactions (bulk)             |
| `POST` | `/transactions/{id}/retry`        | Retry a specific failed transaction     |
| `POST` | `/transactions/{id}/replay`       | Replay any transaction (admin only)     |
| `GET`  | `/transactions/filter`            | Filter transactions by type/status/date|

---

##  Retry Strategy Simulation

Each transaction stores a `retryStrategy` field (e.g., `FIXED`, `EXPONENTIAL`, etc.).  
Based on this, retry behavior changes dynamically using:

```java
switch (tx.getRetryStrategy()) {
    case "FIXED":
        retryWithFixedBackoff(tx);
    case "EXPONENTIAL":
        retryWithExponentialBackoff(tx);
    case "CIRCUIT_BREAKER":
        retryWithCircuitBreaker(tx);
    case "JITTER":
        retryWithJitterDelay(tx);
}
