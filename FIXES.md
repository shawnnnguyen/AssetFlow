# AssetFlow — Known Flaws & Fix Checklist

Work through these top-to-bottom. Check off each item as you fix it.

---

## MEDIUM

- [ ] **No named `@Async` executor bean**
  - File: `backend/.../streaming/` (or wherever `@EnableAsync` is configured)
  - `@EnableAsync` is set but no `@Bean Executor` is defined. `@Async` methods use the default `SimpleAsyncTaskExecutor` (unbounded, creates a new thread per call).
  - Fix: Add a `ThreadPoolTaskExecutor` bean; set core/max pool size and queue capacity.

- [x] **No client-side input validation in modals**
  - Files: `frontend/src/components/` — all modals (RecordTransactionModal, NewPriceAlertModal, etc.)
  - Inputs are submitted without validation; the user only sees a generic failure if the API rejects them.
  - Fix: Validate required fields, numeric ranges (quantity > 0, price > 0), and show field-level error messages before calling the API.

---

## LOW

- [ ] **No pagination on holdings list**
  - File: `backend/.../holdings/HoldingService.java`
  - `findByPortfolioId()` returns all holdings in one query. Fine now; becomes a problem at scale.
  - Fix: Add `Pageable` parameter; return `Page<HoldingResponse>`.

- [x] **No loading/disabled state on form submissions**
  - Files: All modals in `frontend/src/components/`
  - Submit button stays enabled during in-flight requests → duplicate submissions possible.
  - Fix: Add `isSubmitting` boolean state; disable the submit button while `true`.

- [x] **No offline / disconnect detection in the frontend**
  - File: `frontend/src/pages/DashboardPage.tsx` (or a new hook)
  - When the backend is unreachable, the user receives no indication.
  - Fix: Show a banner when STOMP connections drop or a health-check ping fails.

---

## Notes

- Swagger UI: `http://localhost:8080/swagger` — useful for manually testing backend fixes.
- After fixing backend issues, rerun `mvn test` to catch regressions.
- After fixing frontend issues, rerun `npm run typecheck` and `npm run lint`.
