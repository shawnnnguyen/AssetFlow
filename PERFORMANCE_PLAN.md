# AssetFlow Backend Performance Optimization Plan

## Context

The goal is to make the AssetFlow backend reliably handle 100 concurrent users. Three areas drive failure: database overload from N+1 queries and missing indexes, thread starvation from misconfigured async/connection pools, and cache-less repeated computation on every request. The changes below are ordered by performance-per-effort ratio — the first two tiers alone should give an order-of-magnitude improvement.

---

## Status Summary

| # | Item | Status |
|---|---|---|
| 1 | Add Database Indexes | ✅ Done |
| 2 | Fix HikariCP Connection Pool | ✅ Done |
| 3 | Fix N+1 Queries with JOIN FETCH | ✅ Done |
| 4 | Change @ManyToOne to LAZY | ✅ Done |
| 5 | Cache JWT Signing Key | ✅ Done |
| 6 | Cache Exchange Rates | ✅ Done |
| 7 | Custom Async Executor for Portfolio Streaming | ✅ Done |
| 8 | Decouple DB Writes from Finnhub WebSocket Thread | ✅ Done |
| 9 | Fetch livePrices Once Per Scheduler Tick | ✅ Done (already correct) |
| 10 | Add Pessimistic Lock Timeout | ✅ Done |
| 11 | Async CSV Seeding | ✅ Done |
| 12 | Tune Tomcat Thread Pool | ✅ Done |

---

## Tier 1 — Critical (do these first; highest ROI, mostly config/annotation changes)

### 1. ✅ Add Database Indexes

`@Index` / `@Table(indexes = {...})` added to all entity classes. Verified in:
- `Portfolio.java` — `idx_portfolio_user` on `user_id`
- `Holding.java` — `idx_holding_portfolio_asset` on `(portfolio_id, asset_id)`
- `Transaction.java`, `CashTransaction.java`, `PriceAlert.java`, `Price.java` — indexes added per plan

### 2. ✅ Fix HikariCP Connection Pool

Configured in `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.idle-timeout=600000
```

### 3. ✅ Fix N+1 Queries with JOIN FETCH

- `HoldingRepository` — `findByPortfolioIdWithDetails` with `JOIN FETCH h.asset JOIN FETCH h.portfolio`
- `PortfolioRepository` — `findByUserIdWithCurrency` with `JOIN FETCH p.currency`
- `PriceAlertRepository` — extended to also JOIN FETCH user

Services updated to use the new query methods.

### 4. ✅ Change @ManyToOne Default Fetch to LAZY

All `@ManyToOne` across `CashTransaction`, `Holding`, `Asset`, `Price`, `Transaction`, `AlertTriggered` changed to `fetch = FetchType.LAZY`.

### 5. ✅ Cache the JWT Signing Key

`JwtService.getSignInKey()` now decodes Base64 once at bean construction and returns the cached `Key` field on subsequent calls.

---

## Tier 2 — High Impact (async/concurrency fixes)

### 6. ✅ Cache Exchange Rates

`CurrencyConversionService` pre-loads exchange rates into a `ConcurrentHashMap` at `@PostConstruct` and refreshes on a `@Scheduled` interval. DB hits eliminated for the hot conversion path.

### 7. ✅ Define a Custom Async Executor for Portfolio Streaming

`handleAssetPriceUpdate()` now uses `@Async("portfolioCalcExecutor")`. Executor defined in `streaming/config/AsyncConfig.java` (corePool=4, maxPool=10, queue=200).

### 8. ✅ Decouple DB Writes from the Finnhub WebSocket Thread

`MarketDataService.processTrades()` annotated with `@Async("priceWriteExecutor")`. Executor defined in `streaming/config/AsyncConfig.java` (corePool=2, maxPool=4, queue=500). The WebSocket I/O thread now returns immediately; DB writes happen on a `PriceWrite-*` thread.

### 9. ✅ Fetch livePrices Once Per Scheduler Tick

`processPendingPortfolios()` in `PortfolioStreamingService` already builds the `livePrices` map once before the per-portfolio loop. No change needed.

### 10. ✅ Add Pessimistic Lock Timeout

Configured in `application.properties`:
```properties
spring.jpa.properties.jakarta.persistence.lock.timeout=5000
```

---

## Tier 3 — Medium Impact (worthwhile but not blocking)

### 11. ✅ Async CSV Seeding

`DatabaseSeederService.seedData()` annotated with `@Async`. The `CommandLineRunner` in `CsvDataSeeder` calls it through Spring's proxy, so it submits to the async executor and returns immediately — startup no longer blocks on CSV parsing and DB inserts.

### 12. ✅ Tune Tomcat Thread Pool

Configured in `application.properties`:
```properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20
server.tomcat.accept-count=100
```

---

## k6 Load Test Strategy

### Step 1 — Baseline (no load)
Run a single VU for 60s hitting the most expensive endpoints. Record p50/p99 latency as baseline.

### Step 2 — Ramp to 100 VUs (find the breaking point)
```js
// k6 script skeleton
export const options = {
  stages: [
    { duration: '2m', target: 25 },   // warm up
    { duration: '3m', target: 50 },   // mid load
    { duration: '3m', target: 100 },  // target load
    { duration: '2m', target: 100 },  // sustained
    { duration: '1m', target: 0 },    // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests under 500ms
    http_req_failed:   ['rate<0.01'],  // <1% errors
  },
};
```

### Step 3 — Spike test (find ceiling)
After Tier 1 fixes are done, run a spike to 200–300 VUs for 30s to identify the true ceiling.

### Key endpoints to include in the k6 script (in order of importance):
1. `GET /portfolios/{id}/holdings` — exercises N+1 fix
2. `POST /transactions` — exercises pessimistic lock + currency conversion
3. `GET /portfolios` — exercises portfolio + currency JOIN FETCH
4. `GET /alerts` — exercises alert + user JOIN FETCH
5. `GET /transactions` — exercises pagination queries

---

## Grafana Panels to Watch During k6 Runs

### Database (highest priority)
| Panel | Metric | Alert threshold |
|---|---|---|
| Active DB connections | `hikaricp_connections_active` | >25 (approaching pool limit) |
| Connection wait time | `hikaricp_connections_pending` | >0 sustained |
| Query rate | `hibernate_sessions_opened_total` rate | Spikes = N+1 |
| Transaction rate | JPA transaction count | Baseline reference |

### JVM / Memory
| Panel | Metric | Watch for |
|---|---|---|
| Heap used | `jvm_memory_used_bytes{area="heap"}` | GC pauses = memory churn from Eager loading |
| GC pause duration | `jvm_gc_pause_seconds` | Spikes during load = object allocation storm |
| Thread states | `jvm_threads_states` | "WAITING" spike = connection pool starvation |

### HTTP / Latency
| Panel | Metric | Target |
|---|---|---|
| p95 response time | Micrometer `http_server_requests_seconds` p95 | <500ms |
| Error rate | HTTP 5xx / total | <1% |
| Request rate | `http_server_requests_seconds_count` | Match k6 VU ramp |

### WebSocket / Streaming
| Panel | Metric | Watch for |
|---|---|---|
| STOMP broker queue depth | (custom micrometer gauge if added) | Queue growth under load |
| Price update lag | Time from Finnhub event → STOMP push | Should stay <100ms |

### CPU
| Panel | Metric | Watch for |
|---|---|---|
| Process CPU | `process_cpu_usage` | Sustained >80% = thread pool or GC issue |
| System CPU | `system_cpu_usage` | Reference |

### Correlation rule
Watch for **hikaricp_connections_active spikes** co-occurring with **http_req_duration p95 spikes** in k6 — that pattern confirms DB contention as the root cause. After adding indexes and fixing N+1, you should see the active connection count drop by 50–70% at equivalent VU count.

---

## Verification

1. Run k6 baseline test before any changes — record p95 latency and error rate.
2. Apply Tier 1 changes (indexes, HikariCP, N+1 fixes, LAZY fetch, JWT key cache).
3. Rebuild + run k6 ramp test to 100 VUs. Expect p95 latency to drop from seconds to <300ms.
4. Apply Tier 2 changes and re-run. Expect sustained load at 100 VUs with <1% error rate.
5. Check Grafana: active connections should stay well below `maximum-pool-size`; heap should not grow unboundedly.
