const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

function getToken() {
  return localStorage.getItem('token');
}

function isTokenExpired(token) {
  if (!token) return true;
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return Date.now() >= payload.exp * 1000;
  } catch {
    return true;
  }
}

async function fetchApi(endpoint, method = 'GET', body = null) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  const response = await fetch(`${BACKEND_URL}${endpoint}`, options);

  if (response.status === 401) {
    if (isTokenExpired(token)) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      window.location.href = '/login';
    }
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  const text = await response.text();
  if (!text) return { success: true };
  try {
    return JSON.parse(text);
  } catch {
    // Backend returned plain text (e.g. addTracking confirmation message) — treat as success.
    return { success: true, message: text };
  }
}

const auth = {
  login:    (creds) => fetchApi('/auth/login',    'POST', creds),
  register: (creds) => fetchApi('/auth/register', 'POST', creds),
};

// Backend reads userId from @AuthenticationPrincipal — no userId in paths.
const portfolios = {
  getAll:         ()                    => fetchApi('/portfolios'),
  getById:        (portfolioId)         => fetchApi(`/portfolios/${portfolioId}`),
  getPerformance: (portfolioId)         => fetchApi(`/portfolios/${portfolioId}/performance`),
  create:         (body)                => fetchApi('/portfolios', 'POST', body),
};

// Holdings: plain List<HoldingResponse> (not paged).
const holdings = {
  getAll: (portfolioId) => fetchApi(`/portfolios/${portfolioId}/holdings`),
};

// Transactions: GET returns Spring Page<TransactionResponse>; callers read .content.
const transactions = {
  getAll:        (size = 20)            => fetchApi(`/transactions?size=${size}`),
  getPortfolio:  (portfolioId, size=10) => fetchApi(`/portfolios/${portfolioId}/transactions?size=${size}`),
  record:        (portfolioId, body)    => fetchApi(`/portfolios/${portfolioId}/transactions`, 'POST', body),
};

// Backend reads userId from @AuthenticationPrincipal — no userId in paths.
const alerts = {
  getAll:  ()                      => fetchApi('/price-alerts'),
  create:  (body)                  => fetchApi('/price-alerts', 'POST', body),
  update:  (alertId, body)         => fetchApi(`/price-alerts/${alertId}`, 'PATCH', body),
  remove:  (alertId)               => fetchApi(`/price-alerts/${alertId}`, 'DELETE'),
};

const market = {
  getTrackedStocks: ()       => fetchApi('/market/tracked-stocks'),
  getProfile:       (ticker) => fetchApi(`/market/profiles/${ticker}`),
  addTracking:      (ticker) => fetchApi(`/market/tracked-stocks/${ticker}`, 'POST'),
  removeTracking:   (ticker) => fetchApi(`/market/tracked-stocks/${ticker}`, 'DELETE'),
};

const cashTransactions = {
  create:    (portfolioId, body)  => fetchApi(`/portfolios/${portfolioId}/cash-transactions`, 'POST', body),
  getAll:    (portfolioId, size = 10) => fetchApi(`/portfolios/${portfolioId}/cash-transactions?size=${size}`),
  getAllGlobal: (size = 50)       => fetchApi(`/cash-transactions?size=${size}`),
};

const currencies = {
  getAll: () => fetchApi('/currencies'),
};

export const api = { auth, portfolios, holdings, transactions, alerts, market, cashTransactions, currencies };
