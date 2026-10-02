# Berq Bank — P2P Money Transfer System

A digital banking application where users log in, monitor their balance and send money instantly via IBAN. The backend focuses on what matters in a payments system: authorization, transfer validation, consistency under concurrent requests, and not leaking personal data.

![CI](https://github.com/berktopal/berq_bank_p2p/actions/workflows/ci.yml/badge.svg)

## Features

- **Secure login** with server-side sessions and in-page error feedback
- **P2P transfers via IBAN** with live recipient lookup (masked surname, e.g. `Berk T***`)
- **Expense analysis:** spending distribution chart (Chart.js)
- **Transaction history** with search and quick-send contacts
- **Dark / light mode** and **Turkish / English** interface

## How a Transfer Works

```
POST /api/transactions/transfer   { senderAccountId, receiverAccountId, amount }
  │
  ├─ 1. Session check ............................. 401 if not logged in
  ├─ 2. Validate amount (> 0, ≤ 2 decimals) and that the accounts differ
  ├─ 3. Lock both account rows (PESSIMISTIC_WRITE, ascending id order → no deadlocks)
  ├─ 4. Sender account must belong to the user .... 403 otherwise
  ├─ 5. Same currency, sufficient balance
  ├─ 6. Debit sender, credit receiver
  └─ 7. Save the transaction record   ── all in a single @Transactional unit
```

Row locking guarantees that two simultaneous transfers cannot both pass the balance check and overdraw the account (double spending).

## Security

- **Server-side sessions:** HttpOnly, SameSite=Strict cookie; every account and transfer endpoint requires it
- **Ownership checks:** users only see their own accounts and transactions and can only send from accounts they own
- **Password hashing:** BCrypt (legacy plaintext records are upgraded on the next successful login)
- **Data minimization:** DTO responses never expose passwords, TCKN, e-mail or other users' balances
- **XSS protection:** user-provided values are escaped before rendering

## API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users` | Register (password ≥ 8 characters) |
| POST | `/api/users/login` | Log in, starts a session |
| POST | `/api/users/logout` | End the session |
| GET | `/api/accounts` | Current user's accounts |
| POST | `/api/accounts` | Open an account for the current user (starts with a zero balance) |
| GET | `/api/accounts/iban/{iban}` | Recipient lookup → account id + masked name |
| GET | `/api/transactions` | Current user's transactions |
| POST | `/api/transactions/transfer` | Send money |

## Tech Stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Security Crypto (BCrypt), Lombok |
| Database | PostgreSQL |
| Frontend | HTML5, CSS3, vanilla JavaScript, Chart.js, SweetAlert2 |
| Testing / CI | JUnit 5, Spring Boot Test, GitHub Actions with PostgreSQL |

## Project Structure

```
p2p-transfer/src/main/java/p2p_transfer/
├── controller/   # User, Account, Transaction REST endpoints
├── service/      # business rules: login, account opening, transfers
├── repository/   # Spring Data JPA (row locking, per-user queries)
├── entity/       # User, Account, Transaction
├── dto/          # API response models
└── security/     # session helper, exception → HTTP status mapping
p2p-transfer/src/main/resources/static/   # dashboard (HTML/CSS/JS)
```

## Getting Started

### Prerequisites
- JDK 21, PostgreSQL

### Run
```bash
createdb p2p_db                         # or create it from your PostgreSQL client
cd p2p-transfer
export DB_PASSWORD=your_postgres_password
./mvnw spring-boot:run                  # http://localhost:8080
```

Register users with `POST /api/users`, log in, and open accounts with `POST /api/accounts`. New accounts start with a zero balance; seed demo balances directly in the database.

### Tests
```bash
cd p2p-transfer
./mvnw verify      # needs a running PostgreSQL with the p2p_db database
```

`SecurityRulesTests` covers password hashing, legacy password upgrade, negative amounts, transfers from another user's account, insufficient balance and per-user transaction visibility.
