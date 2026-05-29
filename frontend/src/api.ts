import type {
  AuthResponse,
  LoginCredentials,
  RegisterCredentials,
  Portfolio,
  Holding,
  PortfolioPerf,
  Transaction,
  TransactionRequest,
  PriceAlert,
  TrackedStock,
  CompanyProfile,
  CashTransaction,
  CashTransactionRequest,
  Currency,
  SpringPage,
  ApiSuccess,
} from './types';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE' | 'PUT';

interface JwtPayload {
  exp: number;
}

function getToken(): string | null {
  return localStorage.getItem('token');
}

function isTokenExpired(token: string | null): boolean {
  if (!token) return true;
  try {
    const part = token.split('.')[1];
    if (!part) return true;
    const payload = JSON.parse(
      atob(part.replace(/-/g, '+').replace(/_/g, '/'))
    ) as JwtPayload;
    return Date.now() >= payload.exp * 1000;
  } catch {
    return true;
  }
}

function buildRequestInit(method: HttpMethod, body: unknown): RequestInit {
  const token = getToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const options: RequestInit = { method, headers };
  if (body !== null) options.body = JSON.stringify(body);
  return options;
}

function handle401(token: string | null): never {
  if (isTokenExpired(token)) {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    window.location.href = '/login';
  }
  throw new Error('Unauthorized');
}

// For endpoints that return a JSON body.
// The `as T` cast is the single sanctioned unsafe boundary for JSON responses.
async function fetchJson<T>(
  endpoint: string,
  method: HttpMethod = 'GET',
  body: unknown = null,
): Promise<T> {
  const token = getToken();
  const response = await fetch(`${BACKEND_URL}${endpoint}`, buildRequestInit(method, body));

  if (response.status === 401) handle401(token);

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : null) as T;
}

// For endpoints that return 204 / plain text (DELETE, some POST).
async function fetchVoid(
  endpoint: string,
  method: HttpMethod,
  body: unknown = null,
): Promise<ApiSuccess> {
  const token = getToken();
  const response = await fetch(`${BACKEND_URL}${endpoint}`, buildRequestInit(method, body));

  if (response.status === 401) handle401(token);

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  await response.text();
  return { success: true };
}

// ── API namespaces ────────────────────────────────────────────────────────────

const auth = {
  login:    (creds: LoginCredentials)    => fetchJson<AuthResponse>('/auth/login',    'POST', creds),
  register: (creds: RegisterCredentials) => fetchJson<AuthResponse>('/auth/register', 'POST', creds),
};

// Backend resolves userId from @AuthenticationPrincipal — no userId in paths.
const portfolios = {
  getAll:         ()                                            => fetchJson<Portfolio[]>('/portfolios'),
  getById:        (portfolioId: number)                         => fetchJson<Portfolio>(`/portfolios/${portfolioId}`),
  getPerformance: (portfolioId: number)                         => fetchJson<PortfolioPerf>(`/portfolios/${portfolioId}/performance`),
  create:         (body: { name: string; currencyCode: string }) => fetchJson<Portfolio>('/portfolios', 'POST', body),
};

// Holdings: plain List<HoldingResponse> (not paged).
const holdings = {
  getAll: (portfolioId: number) => fetchJson<Holding[]>(`/portfolios/${portfolioId}/holdings`),
};

// Transactions: GET returns Spring Page<TransactionResponse>; callers read .content.
const transactions = {
  getAll:       (size = 20)                                    => fetchJson<SpringPage<Transaction>>(`/transactions?size=${size}`),
  getPortfolio: (portfolioId: number, size = 10)               => fetchJson<SpringPage<Transaction>>(`/portfolios/${portfolioId}/transactions?size=${size}`),
  record:       (portfolioId: number, body: TransactionRequest) => fetchJson<Transaction>(`/portfolios/${portfolioId}/transactions`, 'POST', body),
};

// Backend resolves userId from @AuthenticationPrincipal — no userId in paths.
const alerts = {
  getAll:  ()                                               => fetchJson<PriceAlert[]>('/price-alerts'),
  create:  (body: { ticker: string; targetPrice: number })  => fetchJson<PriceAlert>('/price-alerts', 'POST', body),
  update:  (alertId: number, body: { targetPrice: number }) => fetchJson<PriceAlert>(`/price-alerts/${alertId}`, 'PATCH', body),
  remove:  (alertId: number)                                => fetchVoid(`/price-alerts/${alertId}`, 'DELETE'),
};

const market = {
  getTrackedStocks: ()               => fetchJson<TrackedStock[]>('/market/tracked-stocks'),
  // Returns null when Finnhub has no profile for the ticker.
  getProfile:       (ticker: string) => fetchJson<CompanyProfile | null>(`/market/profiles/${ticker}`),
  addTracking:      (ticker: string) => fetchVoid(`/market/tracked-stocks/${ticker}`, 'POST'),
  removeTracking:   (ticker: string) => fetchVoid(`/market/tracked-stocks/${ticker}`, 'DELETE'),
};

const cashTransactions = {
  create:      (portfolioId: number, body: CashTransactionRequest) => fetchJson<CashTransaction>(`/portfolios/${portfolioId}/cash-transactions`, 'POST', body),
  getAll:      (portfolioId: number, size = 10)                    => fetchJson<SpringPage<CashTransaction>>(`/portfolios/${portfolioId}/cash-transactions?size=${size}`),
  getAllGlobal: (size = 50)                                         => fetchJson<SpringPage<CashTransaction>>(`/cash-transactions?size=${size}`),
};

const currencies = {
  getAll: () => fetchJson<Currency[]>('/currencies'),
};

export const api = { auth, portfolios, holdings, transactions, alerts, market, cashTransactions, currencies };
