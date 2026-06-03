# AssetFlow — Portfolio & Asset Management Platform

Welcome to AssetFlow, a full-stack portfolio management platform that lets you track holdings, record trades, monitor live market prices, and receive real-time price alerts — all in one dashboard.
Demo: https://assetflows.cc

---

## Schema design 

<img width="1216" height="683" alt="assetflow" src="https://github.com/user-attachments/assets/87381cae-19af-4053-bd07-687a75e18c4b" />

---

## Main Features

**Portfolio Management:** Create and manage multiple portfolios, each with its own currency and cash balance. Deposit or withdraw cash, and track total performance across all positions.

**Live Market Prices:** Asset prices stream in real-time via Finnhub's WebSocket feed. The dashboard updates live without polling, keeping every holding's current value accurate to the latest trade.

**Buy & Sell Transactions:** Record buy and sell orders against historical or current prices. Holdings and cash balance update atomically — concurrent writes are protected with row-level pessimistic locks to prevent double-spends.

**Price Alerts:** Set threshold alerts on any tracked asset. When the live price crosses your target, a notification is pushed instantly to your browser over WebSocket — no page refresh needed.

**Multi-Currency Support:** Portfolios and assets can be denominated in different currencies. Exchange rates are seeded from a CSV on startup and cached in-memory so conversions never hit the database on the hot path.

**Performance Tracking:** Portfolio performance (total value, gain/loss, daily change) is recalculated every second in the background and pushed to each connected client via a dedicated STOMP channel.

---

## Tech Stack

**Backend:** Spring Boot 4 · Java 21 · Spring Security (JWT) · Spring Data JPA · Spring WebSocket (STOMP/SockJS) · PostgreSQL 15 · Caffeine Cache · HikariCP

**Frontend:** React 19 · Vite · TypeScript · @stomp/stompjs · SockJS · TailwindCSS

**Infrastructure:** Docker · Docker Compose · Prometheus · Grafana · Micrometer     

**External API:** Finnhub (WebSocket live trades + HTTP company profiles)

---

## Getting Started

### Prerequisites

- Java 21+
- Node 18+
- Docker & Docker Compose (for the full stack)
- A [Finnhub](https://finnhub.io) API key

### Full Stack (Docker)

```bash
docker compose up --build
```

This starts PostgreSQL, the Spring Boot backend on port `8080`, and the Vite frontend on port `3000`.

### Backend Only

```bash
# from /backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
# fill in DB_URL, DB_USERNAME, DB_PASSWORD, FINNHUB_API_KEY, JWT_KEY

./mvnw spring-boot:run          # Linux/Mac
mvnw.cmd spring-boot:run        # Windows
```

Swagger UI: [http://localhost:8080/swagger](http://localhost:8080/swagger)

### Frontend Only

```bash
# from /frontend
npm install
npm run dev
```

App: [http://localhost:3000](http://localhost:3000)

---

## Environment Variables

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `FINNHUB_API_KEY` | Finnhub market data API key |
| `JWT_KEY` | JWT signing secret |
| `JWT_EXPIRATION` | Token TTL in milliseconds |
| `app.frontend.origin` | Allowed CORS + SockJS origin |
| `VITE_BACKEND_URL` | Frontend → backend base URL (default `http://localhost:8080`) |

---

