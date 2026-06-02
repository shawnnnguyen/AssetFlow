/**
 * Load test — ramps to 500 concurrent users over ~11 minutes.
 *
 * Run:
 *   k6 run k6/load-test.js
 *   k6 run --env BASE_URL=http://your-host:8080 k6/load-test.js
 *
 * Goal: p95 latency < 500ms and error rate < 1% at 500 VUs.
 * Record p95 + error rate BEFORE applying perf fixes as your baseline,
 * then re-run after to measure the improvement.
 */

import { buildUserPool, runScenario } from './helpers.js';

const POOL_SIZE = 50;

export const options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '3m', target: 250 },
    { duration: '3m', target: 500 },
    { duration: '2m', target: 500 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /portfolios}':   ['p(95)<300'],
    'http_req_duration{name:GET /holdings}':     ['p(95)<400'],
    'http_req_duration{name:GET /transactions}': ['p(95)<400'],
    'http_req_duration{name:GET /price-alerts}': ['p(95)<300'],
    'http_req_duration{name:POST /transactions}':['p(95)<500'],
  },
};

export function setup() {
  return buildUserPool(POOL_SIZE);
}

export default function (data) {
  runScenario(data);
}
