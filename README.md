# Tiny Ledger

A small Spring Boot REST API for a ledger with transaction history, validation, persistence, and reporting.

## Requirements

- Java 17
- Apache Maven 3.6.3 or newer

## Run

From the project root, run:

```powershell
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

The default account starts with no transactions. To start the app with sample data, use the Maven `seed-data` profile:

```powershell
mvn -Pseed-data spring-boot:run
```

The sample profile creates a €100.00 deposit, a €20.50 withdrawal, and a €25.00 deposit for the default account.

Explore and try the endpoints in Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

## API

Get the current balance:

```http
GET http://localhost:8080/api/balance
```

Example response:

```json
{ "balance": 0.0, "currency": "EUR" }
```

Record a deposit or withdrawal:

```http
POST http://localhost:8080/api/transactions
Content-Type: application/json

{"type":"DEPOSIT","amount":25.00}
```

Use `WITHDRAWAL` for a withdrawal. Successful requests return `201 Created` with the recorded transaction, including its type, amount, and creation time.

Get the transaction history:

```http
GET http://localhost:8080/api/transactions
```

Transactions are returned oldest first. Amounts must be positive, use at most two decimal places, and a withdrawal cannot exceed the current balance. Invalid requests return `400 Bad Request`.

Get a monthly summary:

```http
GET http://localhost:8080/api/reports/monthly?year=2026&month=10
```

Example response:

```json
{ "year": 2026, "month": 10, "income": 100.0, "expenses": 20.5, "net": 79.5 }
```

The ledger stores transactions in an in-memory H2 database. Data remains available while the application is running and resets when it restarts.

The original `/api/balance`, `/api/transactions`, and `/api/reports/monthly` routes use the default account. Account-specific routes are also available.

Create an account:

```http
POST http://localhost:8080/api/accounts
```

Use the returned `id` for account-specific requests:

```http
POST http://localhost:8080/api/accounts/1/transactions
Content-Type: application/json

{"type":"DEPOSIT","amount":100.00}
```

Get that account's balance and complete transaction history:

```http
GET http://localhost:8080/api/accounts/1/balance
GET http://localhost:8080/api/accounts/1/transactions
```

Reconcile the stored balance against the sum of the account's transactions:

```http
GET http://localhost:8080/api/accounts/1/reconciliation
```

The response includes the stored balance, calculated balance, and a `consistent` flag. Transaction writes lock the account row and update its balance and transaction in one database transaction; this prevents concurrent withdrawals from spending the same funds. The account balance also has a database constraint preventing negative values. A rejected withdrawal returns `400 Bad Request` with an `Insufficient funds.` detail.

## Tests

Run the tests with:

```powershell
mvn test
```
