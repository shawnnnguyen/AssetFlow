# Performance Report — 500 VU Peak

**Date:** 2026-06-02 (baseline) / 2026-06-03 (post-optimization)
**Tests:** `load-test.js` (stepped ramp, 11 min) + `spike-test.js` (sudden surge, 3.5 min)
**Overall Result:** PASS (0% error rate on both tests) — HikariCP connection pool identified as primary bottleneck

---

## Load Test

**Profile:** Stepped ramp — 100 → 250 → 500 VUs over 11 minutes

The application handled all 500 VUs without crashing or dropping connections. CPU and JVM heap remained within safe limits. Throughput plateaued at ~322 req/s at peak due to HikariCP pool saturation at 10 active connections. p99 reached 357 ms as threads queued for a DB connection.

**Throughput & Latency**
- Peak throughput: ~322 req/s (plateaued — did not scale linearly from 250 → 500 VUs)
- p50 latency: 17.6 ms (stable across all stages)
- p95 latency: 184 ms at 500 VUs
- p99 latency: 357 ms at 500 VUs
- HTTP 5xx error rate: 0 req/s

**CPU**
- ~30% at 100–250 VUs; 60–80% at 500 VUs (not the limiting factor)

**JVM Heap**
- Heap usage: 8–25% (healthy sawtooth GC pattern, no memory leak)
- GC pause time: peak 15 ms (negligible)

**Database / Connection Pool**
- HikariCP active connections: capped at 10 — hard ceiling hit at 500 VUs
- HikariCP idle connections: dropped to 0 under peak load
- Root cause: threads queuing for DB connections → throughput plateau + p99 spike

---

## Spike Test

**Profile:** 50 VUs baseline → surge to 500 VUs in 30 s → sustain 30 s → recover

The system survived the spike with 0% errors and passed both thresholds (`p95 < 2000 ms`, `error rate < 5%`). However, performance degraded severely. The server spun up 222 worker threads, all forced to wait for 10 DB connections — directly causing the p99 spike to 2.73 s.

**Throughput & Latency**
- Peak throughput: ~300 req/s (hard ceiling)
- p50 latency: 255 ms at peak (vs. 17.6 ms in load test)
- p95 latency: 1530 ms at peak (passes threshold, but severe UX degradation)
- p99 latency: 2730 ms at peak
- HTTP 5xx error rate: 0 req/s

**CPU**
- Spiked to 87.1%, hovered 83–87% during the 500 VU hold; recovered cleanly after

**JVM Heap & GC**
- Heap usage: 9–26% (safe throughout)
- GC pause time: peak 32.7 ms (no meaningful latency impact)

**Thread Behaviour**
- Idle baseline: 32 live threads
- At spike onset: climbed to 222 live threads
- 200+ threads immediately queued waiting for a DB connection → direct cause of p99 spike

**Database / Connection Pool**
- HikariCP active connections: hit hard ceiling at 10 (same as load test)
- Root cause: 222 worker threads competing for 10 connections → severe queuing → 2.73 s p99

---

## Root Cause & Fix (Baseline)

Both tests converge on the same bottleneck: **HikariCP pool capped at 10 connections**.

Increase the pool size in `application.properties`:

```properties
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=10
```

---

## Load Test — After HikariCP Optimization (pool size 10 → 50)

**Date:** 2026-06-03
**Profile:** Stepped ramp — 100 → 250 → 500 VUs over 11 minutes
**HikariCP pool size:** 50

The optimization confirmed the connection pool was the bottleneck — the pool now fully saturates at 500 VUs. However, latency thresholds still failed due to cascading resource starvation: all 50 connections are exhausted, blocking threads until the application hits the Tomcat thread ceiling at ~240 threads, at which point requests queue at the web server level.

**Threshold Results**
- `http_req_failed < 1%` — PASSED (0% error rate)
- `http_req_duration p(95) < 500ms` — FAILED
- `GET /portfolios p(95) < 300ms` — FAILED
- `GET /holdings p(95) < 400ms` — FAILED
- `GET /transactions p(95) < 400ms` — FAILED
- `GET /price-alerts p(95) < 300ms` — FAILED
- `POST /transactions p(95) < 500ms` — FAILED

**Throughput & Latency**
- p95 latency: 1500–2000+ ms at 500 VUs (exponential growth from 250 → 500 VU stage)
- p99 latency: 2000+ ms at peak
- HTTP 5xx error rate: 0 req/s (requests queued rather than dropped)

**CPU**
- 31–47% at 100–250 VUs; peaked at 80% at 500 VUs (not the primary bottleneck)

**JVM Heap & GC**
- Heap usage: stable at 20–25% throughout
- GC pause times: slightly elevated under load but manageable; memory not a bottleneck

**Thread Behaviour**
- Live threads hit a hard ceiling at ~240 at the 500 VU stage
- All threads blocked waiting for a DB connection once the pool was exhausted

**Database / Connection Pool**
- HikariCP active connections: maxed out and flatlined at 50 (new pool ceiling)
- PostgreSQL active connections: mirrored HikariCP at exactly 50
- Root cause: 500 VUs exhausting 50 connections → threads block → thread pool hits 240 ceiling → requests queue at web server → exponential p95/p99 spike

**Analysis**

Increasing the pool from 10 → 50 shifted the bottleneck rather than eliminating it. At 500 VUs the system saturates all 50 connections and all ~240 Tomcat threads simultaneously, producing the same cascading stall pattern as before at a higher ceiling. Further gains require fewer blocking DB operations per request (read caching, query reduction) or reducing the per-request DB round-trips.

---

## Spike Test — After HikariCP Optimization (pool size 10 → 50)

**Date:** 2026-06-03
**Profile:** 50 VUs baseline → surge to 500 VUs in 30 s → sustain 30 s → recover
**HikariCP pool size:** 50

The spike test confirms the architectural limits identified in the gradual load test, but highlights how rapidly those limits are hit under sudden stress. The application survived without crashing or dropping a single request, but latency exceeded the 2000 ms threshold as resource contention gridlocked the system within seconds of the spike onset.

**Threshold Results**
- `http_req_failed < 5%` — PASSED (0% error rate)
- `http_req_duration p(95) < 2000ms` — FAILED (peaked at 2500+ ms)

**Throughput & Latency**
- p95 latency: 2500+ ms at peak (exceeded 2000 ms threshold)
- HTTP 5xx error rate: 0 req/s (all requests queued and eventually processed)

**CPU**
- ~36% during baseline; jumped to 80% at spike onset; peaked at 86.0% during max load
- Dropped back to <2% immediately after test concluded

**JVM Heap & GC**
- Heap usage: stable at ~11.8% throughout
- GC pause time: minor bump to ~40 ms during spike; recovered quickly; memory not a bottleneck

**Thread Behaviour**
- Baseline: 60 live threads
- At spike onset (02:00:00): jumped to 191 threads within seconds
- By 02:00:10: hit hard ceiling of 240 threads — maxed out for the entire spike duration
- Recovery at 02:03:20: thread count returned to 59 once load dropped back to 50 VUs

**Database / Connection Pool**
- HikariCP active connections: instantly maxed out and flatlined at 50 during the spike
- PostgreSQL active connections: mirrored HikariCP, saturated for the full spike duration

**Analysis**

The spike test confirms the same cascading failure pattern observed in the load test, compressed into 10 seconds:
- 500 VUs immediately exhausted all 50 HikariCP connections
- Blocked threads forced the web server to spawn new threads until the 240-thread ceiling was hit within 10 seconds of spike onset
- With threads maxed and all connections locked, requests piled up in the web server queue → p95 beyond 2.5 s
- No crashes, no OOM errors, no dropped requests — the system gridlocked gracefully and fully recovered once load returned to 50 VUs
