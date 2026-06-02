# Performance Report — 500 VU Peak

**Date:** 2026-06-02
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
