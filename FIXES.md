# AssetFlow — Known Flaws & Fix Checklist

Work through these top-to-bottom. Check off each item as you fix it.

---

## HIGH

- [x] **Null check missing on `currencyRepository.findByCode()`**
  - Files: `backend/src/main/java/com/project3/AssetFlow/portfolio/PortfolioService.java`, `market/MarketDataService.java`
  - All three call sites now throw `ResponseStatusException(BAD_REQUEST)` on unknown currency code. Also fixed a pre-existing bug in `updateVerifiedPortfolio` where the error message logged the null object instead of the currency code string.

- [x] **`CurrencyConversionService` throws `IllegalStateException` → returns 500**
  - File: `backend/.../currency/CurrencyConversionService.java`
  - Replaced `IllegalStateException` with `ResponseStatusException(422 UNPROCESSABLE_ENTITY)` directly in the service, consistent with how other domain errors are raised. Also removed the generic `IllegalStateException` handler from `GlobalExceptionHandler` — it was masking the root issue and could swallow unrelated exceptions as 400s.

- [x] **No JWT refresh endpoint**
  - File: `backend/.../identity/AuthResource.java`
  - Expired tokens require full re-login. Frontend handles it via redirect (acceptable short-term), but there is no `/auth/refresh` endpoint.
  - Fix: Add `POST /auth/refresh` that accepts a valid (non-expired) refresh token and returns a new access token, or at minimum document the intended flow.

---

## MEDIUM

- [x] **`BACKEND_URL` duplicated across frontend hooks**
  - Files: `frontend/src/api.ts`, `hooks/useMarketWebSocket.ts`, `hooks/usePortfolioWebSocket.ts`, `hooks/useAlertWebSocket.ts`
  - Each defines its own `const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'`.
  - Fix: Export `BACKEND_URL` from `api.ts`; import it in all three hooks.

- [ ] **No named `@Async` executor bean**
  - File: `backend/.../streaming/` (or wherever `@EnableAsync` is configured)
  - `@EnableAsync` is set but no `@Bean Executor` is defined. `@Async` methods use the default `SimpleAsyncTaskExecutor` (unbounded, creates a new thread per call).
  - Fix: Add a `ThreadPoolTaskExecutor` bean; set core/max pool size and queue capacity.

- [ ] **Silent API failures in frontend hooks — no UI error feedback**
  - Files: `frontend/src/hooks/usePortfolios.ts`, `useHoldings.ts`, `useTransactions.ts`, others
  - Failed API calls are swallowed with `.catch(console.error)`; the user sees nothing.
  - Fix: Add an `error` state to each hook (or a global toast context); surface errors in the UI.

- [ ] **No client-side input validation in modals**
  - Files: `frontend/src/components/` — all modals (RecordTransactionModal, NewPriceAlertModal, etc.)
  - Inputs are submitted without validation; the user only sees a generic failure if the API rejects them.
  - Fix: Validate required fields, numeric ranges (quantity > 0, price > 0), and show field-level error messages before calling the API.

- [ ] **`atob()` token parsing has no error boundary**
  - File: `frontend/src/api.ts` (~line 35–44)
  - `JSON.parse(atob(...))` throws on malformed tokens; the catch-all silently returns `true` (token treated as expired).
  - Fix: `catch (e) { console.warn('Invalid token format', e); return true; }` — already returns true, just add the log so malformed tokens surface in dev tools.

---

## LOW

- [ ] **No pagination on holdings list**
  - File: `backend/.../holdings/HoldingService.java`
  - `findByPortfolioId()` returns all holdings in one query. Fine now; becomes a problem at scale.
  - Fix: Add `Pageable` parameter; return `Page<HoldingResponse>`.

- [x] **`@Scheduled(fixedRate = 1000)` magic number**
  - File: `backend/.../portfolio/PortfolioStreamingService.java`
  - Hardcoded 1000 ms recalc interval.
  - Fix: `@Scheduled(fixedRateString = "${app.portfolio.performance-recalc-interval-ms:1000}")`

- [ ] **No loading/disabled state on form submissions**
  - Files: All modals in `frontend/src/components/`
  - Submit button stays enabled during in-flight requests → duplicate submissions possible.
  - Fix: Add `isSubmitting` boolean state; disable the submit button while `true`.

- [ ] **No offline / disconnect detection in the frontend**
  - File: `frontend/src/pages/DashboardPage.tsx` (or a new hook)
  - When the backend is unreachable, the user receives no indication.
  - Fix: Show a banner when STOMP connections drop or a health-check ping fails.

- [ ] **No Hikari connection pool tuning**
  - File: `backend/src/main/resources/application.properties`
  - Running with Spring Boot defaults (10 connections max).
  - Fix: Add `spring.datasource.hikari.maximum-pool-size=20` (tune to your DB server's capacity).

- [ ] **No meaningful test coverage**
  - File: `backend/src/test/` — only a context-load test exists
  - Zero unit/integration tests for any service.
  - Priority test targets:
    1. `TransactionService` — BUY with insufficient cash, SELL with insufficient quantity, average cost calc
    2. `CashTransactionService` — withdrawal below balance
    3. `PriceAlertService` — threshold crossing, duplicate prevention
    4. `AuthService` — duplicate username/email, wrong password
    5. `PortfolioService` — performance calculation, currency conversion

---

## Notes

- Swagger UI: `http://localhost:8080/swagger` — useful for manually testing backend fixes.
- After fixing backend issues, rerun `mvn test` to catch regressions.
- After fixing frontend issues, rerun `npm run typecheck` and `npm run lint`.
