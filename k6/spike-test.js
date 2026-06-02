/**
 * Spike test — hits 500 VUs for 30s to find the true throughput ceiling.
 * Run this AFTER the load test passes to identify where the system breaks.
 *
 * Run:
 *   k6 run k6/spike-test.js
 *   k6 run --env BASE_URL=http://your-host:8080 k6/spike-test.js
 *
 * What to watch in Grafana during this test:
 *   - hikaricp_connections_active  → should not hit 30 (the pool ceiling)
 *   - jvm_threads_states{state="WAITING"} → spike = connection pool starvation
 *   - http_req_duration p95 in k6 output
 *   - HTTP 5xx rate
 */

import { buildUserPool, runScenario } from './helpers.js';

const POOL_SIZE = 50;

export const options = {
  stages: [
    { duration: '1m',  target: 50  },
    { duration: '30s', target: 500 },
    { duration: '30s', target: 500 },
    { duration: '1m',  target: 50  },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  return buildUserPool(POOL_SIZE);
}

export default function (data) {
  runScenario(data);
}
