# Optimization Plan — 500 VU p95 Pass

**Date:** 2026-06-03
**Goal:** Pass all k6 p95 latency thresholds at 500 VUs (GET < 300–400ms, POST < 500ms, spike p95 < 2000ms)

---

## Baseline Corrections

The `performance-report-500vu.md` describes an older build. Live code already has:
- HikariCP pool = 30 (not 50 as reported)
- Tomcat max threads = 200
- Exchange rate cache already in `CurrencyConversionService`
- Write decoupling from WS thread already in `AsyncConfig` / `AlertDispatchConfig`
- List queries already use `JOIN FETCH`

**Re-run baseline load test against the current build before any changes.**

---

## Phase 1 — Reduce Connection Hold-Time per Request

> Prerequisite for all other phases. Cuts how long each request occupies a DB connection.

### 1.1 — Fix `getVerifiedPortfolio` lazy fetches

**File:** `PortfolioRepository.java`, `PortfolioService.java`

Add a `findByIdWithDetails` query with `JOIN FETCH user` and `JOIN FETCH currency`, then use it in `getVerifiedPortfolio()`.

Currently the method calls plain `findById()` then lazily touches `portfolio.getUser()` and `portfolio.getCurrency()` — 3 separate DB round-trips per call. Every `POST /transactions`, `GET /portfolios/{id}`, and alert operation pays this cost.

**Must be done before 1.2.**

- [x] Add `findByIdWithDetails` to `PortfolioRepository`
- [x] Update `getVerifiedPortfolio` to use it

### 1.2 — Disable Open-In-View

**File:** `application.properties`

```properties
spring.jpa.open-in-view=false
```

OSIV holds a DB connection for the full request lifecycle including JSON serialization. May be the single largest win. Safe only after 1.1 eliminates the remaining lazy fetches.

- [x] Set `spring.jpa.open-in-view=false`
- [x] Verify no `LazyInitializationException` on all endpoints

### 1.3 — Add `@Transactional(readOnly = true)` to read service methods

**Files:** `PortfolioService`, `HoldingService`, `TransactionService`, `PriceAlertService`

Without `readOnly`, each repository call in a service method checks out and returns a connection separately. `readOnly` also disables Hibernate dirty-checking.

- [ ] Audit all read service methods
- [ ] Annotate with `@Transactional(readOnly = true)`

### 1.4 — Verify DB indexes on hot columns

Check that indexes exist on every FK/filter column hit by hot queries:

| Table | Column(s) |
|---|---|
| `holding` | `portfolio_id`, `asset_id` |
| `transaction` | `user_id`, `(portfolio_id, asset_id)` |
| `price_alert` | `(asset_id, enabled)` |
| `portfolio` | `user_id` |

Missing indexes → seq scans → longer connection hold time → amplifies everything else.

- [x] Audit schema / migration scripts for missing indexes
- [x] Add any missing indexes

---

## Phase 2 — Remove the Thread Ceiling

> Must ship together with or after Phase 1. VT alone without reduced connection hold-time just trades a thread queue for a connection queue.

### 2.1 — Enable Java 21 Virtual Threads

**File:** `application.properties`

```properties
spring.threads.virtual.enabled=true
```

Removes the ~200 Tomcat thread hard ceiling at zero code change. Virtual threads park cheaply while waiting for a DB connection.

**JDK pinning risk:** HikariCP uses `synchronized` internally which can pin virtual thread carrier threads on JDK 21. Run the load test with `-Djdk.tracePinnedThreads=short`. On JDK 24+ this is largely fixed.

- [ ] Enable virtual threads
- [ ] Run load test with `-Djdk.tracePinnedThreads=short`
- [ ] Confirm no pinning warnings in output

### 2.2 — Lower HikariCP connection-timeout

**File:** `application.properties`

```properties
spring.datasource.hikari.connection-timeout=3000
```

Default is 20s. With VT removing the thread ceiling, the pool is now the true bottleneck. A 20s timeout lets latency silently accumulate. Fail-fast at 3s gives clean backpressure and bounded tail latency (503 instead of 20s waits).

- [ ] Set `connection-timeout=3000`
- [ ] Confirm k6 threshold config treats 503s as expected shedding if needed

---

## Phase 3 — Collapse DB Demand for Reads

> The item that mathematically makes the thresholds reachable.

### 3.1 — Caffeine read cache on hot GET endpoints

**Dependency:** `com.github.ben-manes.caffeine:caffeine` (add to `pom.xml`)

Cache GET responses for portfolios, holdings, transactions, and price-alerts:
- Key: `userId` or `(userId, portfolioId)`
- TTL: 3–10s
- Invalidate relevant entries on any write operation

**Why it works:** At 500 VUs, if ~85% of reads are cache hits, only ~75 req/s actually reach the 30-connection pool — comfortably under saturation, connection wait drops to ~0, p95 collapses to raw service time (tens of ms).

**Staleness note:** Holdings staleness is acceptable — the authoritative live value arrives via STOMP WebSocket (`/user/queue/portfolio/{id}`) anyway.

**Security note:** Cache must be keyed per user. Never share cache entries across users.

- [ ] Add Caffeine dependency
- [ ] Enable `@EnableCaching` + configure `CaffeineCacheManager`
- [ ] Annotate read service methods with `@Cacheable`
- [ ] Annotate write service methods with `@CacheEvict`
- [ ] Confirm per-user key isolation

---

## Phase 4 — Write Path (only if POST /transactions still fails)

Do **not** implement async 202 for `POST /transactions` — it requires a full idempotency + status-polling design and the pessimistic locks (`PESSIMISTIC_WRITE` on holdings and cash balance) serialize concurrent writes regardless of threading model.

Instead:
- Audit `PESSIMISTIC_WRITE` lock scope — minimize the locked section
- Verify lock acquisition order is consistent (holdings before cash, or vice versa) to prevent deadlocks under concurrent writes to the same portfolio

- [ ] Profile write path under load
- [ ] Audit pessimistic lock scope and ordering

---

## Validation Gates

Run the full k6 suite after each phase. A passing run must show:

| Metric | Target |
|---|---|
| `hikaricp.connections.pending` | ≈ 0 at p95 |
| `pg_stat_activity` active count | ≤ 30 (pool size) |
| Pinned-thread trace output | Empty |
| HTTP 5xx error rate | 0% |
| p95 (all GET endpoints) | < 300–400ms |
| p95 (POST /transactions) | < 500ms |
| Spike test p95 | < 2000ms |

Prometheus/Micrometer is already wired — use the existing Grafana/scrape setup to monitor `hikaricp.*` metrics during the test.

---

## Escalation Path

If all phases are complete and thresholds still fail, the remaining bottleneck is single-node PostgreSQL itself. Next step: add a read replica and route all GET traffic to it via Spring's `AbstractRoutingDataSource`.

---

## Progress Log

| Date | Phase | Status | Notes |
|---|---|---|---|
| 2026-06-03 | — | Planned | Initial plan drafted from load + spike test analysis |
| 2026-06-03 | 1.1 | Done | Added `findByIdWithDetails` (JOIN FETCH user + currency); updated `getVerifiedPortfolio` to use it — eliminates 2 extra round-trips per call |
| 2026-06-03 | 1.2 | Done | Set `open-in-view=false`; added `@Transactional(readOnly=true)` to all read methods in Portfolio/Transaction/CashTransaction/HoldingService; fixed `findByTickerAndPortfolioId` to JOIN FETCH asset+portfolio |
| 2026-06-03 | 1.3 | Done | Covered during 1.2 — all read service methods annotated `@Transactional(readOnly=true)` |
| 2026-06-03 | 1.4 | Done | Added composite `(portfolio_id, asset_id)` index to `transactions`; added composite `(asset_id, enabled)` index to `price_alerts` via `@Index` annotations |
