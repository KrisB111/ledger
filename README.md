# Tiny Ledger

A small Spring Boot REST API for an in-memory ledger.

## Requirements

- Java 17
- Apache Maven 3.6.3 or newer

## Run

From the project root, run:

```powershell
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

Explore and try the endpoints in Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

## API

Get the current balance:

```http
GET http://localhost:8080/api/balance
```

Example response:

```json
{"balance":0.00,"currency":"EUR"}
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

Transactions are returned oldest first. Amounts must be positive, use at most two decimal places, and a withdrawal cannot exceed the current balance. Invalid requests return `400 Bad Request`. The ledger is stored in memory and resets when the application restarts.

## Tests

Run the tests with:

```powershell
mvn test
```