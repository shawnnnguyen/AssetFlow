import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}

export function buildUserPool(poolSize) {
  const runId = Date.now().toString(36);
  const users = [];

  for (let i = 0; i < poolSize; i++) {
    const email = `k6-${runId}-${i}@test.com`;
    const username = `k6${runId}${i}`;
    const password = 'K6TestPass1!';

    const regRes = http.post(
      `${BASE_URL}/auth/register`,
      JSON.stringify({ username, email, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (regRes.status !== 200) {
      console.error(`[setup] register failed for user ${i}: ${regRes.status} ${regRes.body}`);
      continue;
    }
    const { token } = regRes.json();

    const portfolioRes = http.post(
      `${BASE_URL}/portfolios`,
      JSON.stringify({ name: `k6-portfolio-${i}`, currencyCode: 'USD' }),
      authHeaders(token)
    );
    if (portfolioRes.status !== 201) {
      console.error(`[setup] create portfolio failed for user ${i}: ${portfolioRes.status}`);
      continue;
    }
    const portfolioId = portfolioRes.json('id');

    http.post(
      `${BASE_URL}/portfolios/${portfolioId}/cash-transactions`,
      JSON.stringify({ portfolioId, type: 'DEPOSIT', amount: 1000000 }),
      authHeaders(token)
    );

    users.push({ token, portfolioId });
  }

  const stocksRes = http.get(`${BASE_URL}/market/tracked-stocks`);
  const assetIds = stocksRes.status === 200
    ? stocksRes.json().map(s => s.assetId).filter(id => id != null)
    : [];

  console.log(`[setup] pool ready: ${users.length} users, ${assetIds.length} tracked assets`);
  return { users, assetIds, baseUrl: BASE_URL };
}

export function runScenario(data) {
  const { users, assetIds, baseUrl } = data;
  if (!users || users.length === 0) return;

  const { token, portfolioId } = users[(__VU - 1) % users.length];
  const opts = authHeaders(token);

  const portfoliosRes = http.get(`${baseUrl}/portfolios`, {
    ...opts,
    tags: { name: 'GET /portfolios' },
  });
  check(portfoliosRes, { 'GET /portfolios → 200': r => r.status === 200 });

  const holdingsRes = http.get(`${baseUrl}/portfolios/${portfolioId}/holdings`, {
    ...opts,
    tags: { name: 'GET /holdings' },
  });
  check(holdingsRes, { 'GET /holdings → 200': r => r.status === 200 });

  const txGetRes = http.get(`${baseUrl}/portfolios/${portfolioId}/transactions`, {
    ...opts,
    tags: { name: 'GET /transactions' },
  });
  check(txGetRes, { 'GET /transactions → 200': r => r.status === 200 });

  const alertsRes = http.get(`${baseUrl}/price-alerts`, {
    ...opts,
    tags: { name: 'GET /price-alerts' },
  });
  check(alertsRes, { 'GET /price-alerts → 200': r => r.status === 200 });

  if (assetIds.length > 0) {
    const assetId = assetIds[Math.floor(Math.random() * assetIds.length)];
    const txPostRes = http.post(
      `${baseUrl}/portfolios/${portfolioId}/transactions`,
      JSON.stringify({
        assetId,
        portfolioId,
        executedAt: new Date().toISOString(),
        quantity: 0.001,
        type: 'BUY',
      }),
      {
        ...opts,
        tags: { name: 'POST /transactions' },
      }
    );
    check(txPostRes, { 'POST /transactions → 2xx': r => r.status >= 200 && r.status < 300 });
  }

  sleep(1);
}
