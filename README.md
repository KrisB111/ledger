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

## Current API

Get the current balance:

```http
GET http://localhost:8080/api/balance
```

Example response:

```json
{"balance":0.00,"currency":"EUR"}
```

The ledger is currently stored in memory and resets when the application restarts.

## Tests

Run the tests with:

```powershell
mvn test
```