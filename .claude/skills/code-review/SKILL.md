---
name: code-review
description: >
  Comprehensive code review skill for AssetFlow. Analyzes regression risk, JPA/Hibernate correctness,
  WebSocket streaming integrity, Spring Boot configuration safety, and portfolio data accuracy.
  Adapted for Spring Boot 4 / Java 21 / PostgreSQL / Finnhub WebSocket stack.
---

# Code Review

Deep code review for any changeset. Focuses on **regression prevention**, **data integrity**, and
**correctness** — not style nitpicking. Can review a single file, a git diff, a branch/PR, or a
specific task.

## Step 1 — Scope the Review

Determine what to review:

**If reviewing a branch/PR:** Run `git diff master...HEAD --stat` and `git diff master...HEAD` to see
all changes.

**If reviewing specific files:** Read each file in full.

**If reviewing latest changes:** Run `git diff --stat` and `git diff` for unstaged, or
`git diff --cached` for staged changes.

Collect the list of all modified/created files. Read EVERY changed file in full (not just diffs).

## Step 2 — Load References

Read before reviewing:

**Project contracts:**
- `CLAUDE.md` — project overview, conventions, build commands (if it exists)
- `CODE_REVIEW.md` — known bugs and priority fixes (if it exists)

**Build & dependency context:**
- `AssetFlow/backend/pom.xml` — Spring Boot version, dependencies, Java version

**Codebase context (read the relevant ones for the change type):**
- `src/main/java/com/project3/AssetFlow/streaming/handler/FinnhubWebSocketConfig.java` — Finnhub connection setup
- `src/main/java/com/project3/AssetFlow/streaming/handler/FinnhubWebSocketHandler.java` — Trade message parsing
- `src/main/java/com/project3/AssetFlow/streaming/handler/FrontendWebSocketConfig.java` — STOMP/SockJS broker config
- `src/main/java/com/project3/AssetFlow/market/MarketDataService.java` — Trade processing pipeline
- `src/main/java/com/project3/AssetFlow/portfolio/PortfolioService.java` — Portfolio business logic
- `src/main/java/com/project3/AssetFlow/identity/User.java` — User entity & relationships

**Related source files:** Read callers, callees, and imports of changed code to assess blast radius.

All source paths are relative to `AssetFlow/backend/`.

## Step 3 — Regression Analysis (HIGHEST PRIORITY)

This system has a real-time streaming pipeline and financial data persistence where regressions
directly affect data accuracy and user experience.

### 3.1 Streaming Pipeline Impact

The core data flow is:

```
Finnhub WebSocket (external)
  -> FinnhubWebSocketHandler.handleTextMessage()
  -> MarketDataService.processTrades()
  -> priceRepository.save()  (PostgreSQL)
  -> SimpMessagingTemplate.convertAndSend()  (STOMP to frontend)
```

**Verification checklist:**
- [ ] If code touches WebSocket handler: JSON deserialization still correct for Finnhub response format (`{"type":"trade","data":[...]}`)
- [ ] If code touches MarketDataService: trade processing still saves Price AND broadcasts to frontend
- [ ] If code touches FinnhubWebSocketConfig: connection lifecycle (afterConnectionEstablished, subscribe messages) preserved
- [ ] WebSocket topic paths use correct format: `/topic/market/{TICKER}` (not `/topic/market{TICKER}`)
- [ ] Finnhub subscribe message format preserved: `{"type":"subscribe","symbol":"TICKER"}`
- [ ] STOMP message broker prefix `/topic` and app destination prefix `/app` still configured
- [ ] SockJS fallback endpoint `/ws` still registered
- [ ] No blocking I/O introduced in WebSocket handler threads

### 3.2 JPA Entity & Relationship Regression

Entity changes can silently corrupt data or break queries.

**Critical entity relationships:**
- `User` <-> `Portfolio`: `@OneToOne` — verify `mappedBy` is on ONE side only
- `Portfolio` -> `Holding`: `@OneToMany` via `holdings` list
- `Holding` -> `Asset`: `@ManyToOne` via `asset` field
- `Asset` -> `Price`: Linked by `ticker` field (not a JPA relationship)
- `User` -> `Transaction`: `@OneToMany` via `userId`

**Verification checklist:**
- [ ] No new bidirectional relationship without `mappedBy` on one side
- [ ] `@Enumerated(EnumType.STRING)` on ALL enum fields (AssetType, TradeType, Currency)
- [ ] New entity fields have sensible defaults or are nullable
- [ ] `@Column` constraints match database schema (length, nullable, unique)
- [ ] No `cascade = CascadeType.ALL` without careful consideration (orphan deletion risk)
- [ ] `@Transient` fields not confused with persistent fields
- [ ] ID generation strategy consistent (`@GeneratedValue(strategy = GenerationType.IDENTITY)`)
- [ ] No `==` comparison on `Long` wrapper types (use `.equals()`)
- [ ] `hashCode()`/`equals()` based on business key or ID, not all fields

### 3.3 Repository & Query Regression

- [ ] Custom queries use `@Query` with parameterized bindings (no string concatenation)
- [ ] Repository interfaces extend `JpaRepository<Entity, IdType>` (not empty interfaces)
- [ ] `findBy*` method names match actual entity field names (Spring Data naming convention)
- [ ] Pagination used for potentially large result sets (`Pageable` parameter)
- [ ] `@Modifying` + `@Transactional` on UPDATE/DELETE queries

### 3.4 REST API Regression

- [ ] New endpoints don't shadow existing ones (same path + method)
- [ ] `@RequestBody` validated with `@Valid` where applicable
- [ ] Response DTOs don't leak entity internals (circular references, lazy-loaded collections)
- [ ] HTTP status codes correct (201 for create, 204 for delete, 404 for not found)
- [ ] No N+1 query patterns in controller methods (fetch required data in service layer)

## Step 4 — Data Integrity Analysis (CRITICAL — P0 if violated)

Financial data integrity is the highest-severity concern. Price data, portfolio values, and
transaction history must be accurate.

### 4.1 Price Data Pipeline

**Verification checklist:**
- [ ] Price records linked to correct Asset via ticker lookup
- [ ] `assetRepository.findByTicker()` handles unknown tickers (no silent null dereference)
- [ ] Price timestamps consistent (all epoch millis OR all LocalDateTime — not mixed)
- [ ] No duplicate price records for same ticker + timestamp
- [ ] Batch processing doesn't drop trades silently on partial failure

### 4.2 Portfolio Calculations

- [ ] Buy/sell operations update `Holding.quantity` atomically (race condition risk with concurrent trades)
- [ ] Cash balance updated correctly: `DEPOSIT` adds, `WITHDRAWAL` subtracts, `BUY` subtracts, `SELL` adds
- [ ] Portfolio value calculation uses latest price per asset (not stale cached values)
- [ ] Division operations guard against divide-by-zero (percentage calculations)
- [ ] Currency conversion applied consistently (all values in same currency or properly converted)

### 4.3 Transaction Integrity

- [ ] Transaction records immutable after creation (no update endpoints)
- [ ] Every BUY/SELL creates a Transaction record (audit trail)
- [ ] Transaction amount and holding quantity change are consistent
- [ ] `@Transactional` wraps operations that modify multiple entities (Holding + Portfolio + Transaction)

## Step 5 — Accuracy Analysis

### 5.1 Logic Correctness
- [ ] Null checks BEFORE method calls on nullable references (not after)
- [ ] Stream operations handle empty collections (`.findFirst()` returns `Optional`)
- [ ] Conditional logic covers all branches (switch/if-else completeness)
- [ ] Numeric operations use `BigDecimal` for financial calculations (not `double`)
- [ ] Date/time operations use `java.time` API (not `java.util.Date`)
- [ ] Collection operations don't modify during iteration (ConcurrentModificationException risk)

### 5.2 DTO Mapping Accuracy
- [ ] Entity-to-DTO mapping preserves all required fields
- [ ] DTO field names match frontend expectations (JSON property names)
- [ ] Nested objects properly mapped (not just top-level fields)
- [ ] `AssetPerformanceDTO` calculations correct (gain/loss, percentage change)
- [ ] `PortfolioValueDTO` aggregation correct (sum of holdings * current prices)

### 5.3 External API Accuracy
- [ ] Finnhub WebSocket message format matches API documentation
- [ ] Ticker symbols uppercase and trimmed
- [ ] API key passed correctly (not leaked in logs or error messages)
- [ ] Reconnection logic handles Finnhub disconnects gracefully

## Step 6 — Robustness Analysis

### 6.1 Error Handling
- [ ] `@ControllerAdvice` handles exceptions globally (not per-controller try/catch)
- [ ] Custom exceptions for business errors (InsufficientFundsException, AssetNotFoundException)
- [ ] No bare `catch (Exception e)` that swallows errors silently
- [ ] External service failures (Finnhub, DB) logged with context and handled gracefully
- [ ] `@Transactional` rollback on unchecked exceptions (default behavior — verify not overridden)

### 6.2 Resource Management
- [ ] Database connections returned to pool (no manual DataSource usage without try-with-resources)
- [ ] WebSocket sessions tracked and cleaned up on disconnect
- [ ] No memory leaks from unbounded collections (e.g., in-memory price history)
- [ ] `@Scheduled` tasks have proper error handling (exceptions don't kill the scheduler)

### 6.3 Graceful Degradation
- [ ] Finnhub WebSocket disconnect triggers reconnection (not silent failure)
- [ ] Database unavailability doesn't crash the streaming pipeline
- [ ] Frontend WebSocket disconnect handled (STOMP session cleanup)
- [ ] Missing configuration properties fail fast at startup (not at first request)

## Step 7 — Concurrency & Performance Safety

### 7.1 Thread Safety
- [ ] No shared mutable state in `@Service` or `@Component` beans (singleton scope by default)
- [ ] Static mutable collections synchronized or replaced with `ConcurrentHashMap` / `CopyOnWriteArrayList`
- [ ] WebSocket handler thread-safe (multiple messages arrive concurrently)
- [ ] `SimpMessagingTemplate` is thread-safe (OK to use from any thread)
- [ ] No `synchronized` blocks holding database connections (deadlock risk)

### 7.2 Database Performance
- [ ] Batch inserts for high-frequency writes (`spring.jpa.properties.hibernate.jdbc.batch_size`)
- [ ] Indexes on frequently queried columns (`ticker`, `userId`, `portfolioId`)
- [ ] `@Transactional(readOnly = true)` on read-only service methods
- [ ] Lazy loading not triggered in controller layer (N+1 query via serialization)
- [ ] Connection pool sized appropriately for concurrent WebSocket + REST traffic

### 7.3 WebSocket Performance
- [ ] Message broadcasting throttled for high-frequency tickers (not every trade)
- [ ] JSON serialization reuses ObjectMapper (not creating new instance per message)
- [ ] Frontend subscription to specific tickers, not all market data
- [ ] SockJS fallback doesn't degrade to polling unnecessarily

## Step 8 — Type Safety & Java Conventions

### 8.1 Java 21 Conventions
- [ ] Records used for DTOs and value objects (immutable by default)
- [ ] `sealed` classes/interfaces for closed type hierarchies where appropriate
- [ ] Pattern matching in `instanceof` checks (`if (obj instanceof Foo foo)`)
- [ ] Text blocks for multi-line strings (SQL queries, JSON templates)
- [ ] `var` used judiciously (only when type is obvious from right-hand side)

### 8.2 Spring Boot Conventions
- [ ] Constructor injection (no field `@Autowired` — use `@RequiredArgsConstructor` or explicit constructor)
- [ ] `@ConfigurationProperties` for grouped config (not scattered `@Value`)
- [ ] Profiles for environment-specific config (`application-dev.properties`, `application-prod.properties`)
- [ ] `@Validated` on `@ConfigurationProperties` classes
- [ ] Bean methods in `@Configuration` classes (not `@Component` with `@Bean`)

### 8.3 Lombok Usage
- [ ] `@Data` not used on JPA entities (breaks equals/hashCode with lazy loading)
- [ ] `@Builder` not used without `@NoArgsConstructor` on JPA entities (Hibernate needs default constructor)
- [ ] `@Slf4j` instead of manual logger declaration
- [ ] `@RequiredArgsConstructor` for dependency injection (final fields)

## Step 9 — Testing Quality

### 9.1 Test Existence & Coverage
- [ ] Tests exist for every new/modified service method
- [ ] Happy path tested
- [ ] At least one error case tested (invalid input, missing entity, DB failure)
- [ ] Edge cases tested (empty portfolio, zero quantity, negative values)
- [ ] WebSocket handler tests verify message parsing

### 9.2 Test Patterns
- [ ] `@SpringBootTest` only for integration tests (use `@ExtendWith(MockitoExtension.class)` for unit tests)
- [ ] `@DataJpaTest` for repository tests (auto-configures in-memory DB)
- [ ] `@WebMvcTest` for controller tests (no full context loading)
- [ ] Mocks for external dependencies (Finnhub, database in unit tests)
- [ ] `@Transactional` on integration tests (auto-rollback)
- [ ] Assertions use AssertJ fluent API (preferred over JUnit assertions)

### 9.3 Test Gaps (Flag as [TEST])
- [ ] Changed entity without repository test
- [ ] Changed service logic without unit test
- [ ] Changed WebSocket handling without handler test
- [ ] Changed REST endpoint without controller test
- [ ] Changed financial calculation without accuracy test

## Step 10 — Security Analysis

### 10.1 Secrets & Configuration
- [ ] No API keys, tokens, or credentials hardcoded in source files
- [ ] Secrets loaded via environment variables (`${ENV_VAR}` in properties)
- [ ] `application.properties` not committed with real credentials
- [ ] Finnhub API key not logged or exposed in error responses
- [ ] `.gitignore` includes `application-local.properties`, `.env`

### 10.2 Input Validation
- [ ] `@Valid` on `@RequestBody` parameters
- [ ] Bean Validation annotations on DTOs (`@NotNull`, `@NotBlank`, `@Min`, `@Max`)
- [ ] Path variables validated (ticker format, positive IDs)
- [ ] No raw user input in SQL queries (use parameterized queries or Spring Data methods)
- [ ] No user input in WebSocket topic paths without sanitization

### 10.3 Authentication & Authorization
- [ ] Endpoints that modify data require authentication
- [ ] User can only access their own portfolio (not other users' data)
- [ ] Admin endpoints (if any) require elevated permissions
- [ ] CORS configured to allow only expected origins

### 10.4 Logging Safety
- [ ] SLF4J used (`@Slf4j`) — no `System.out.println` or `System.err.println`
- [ ] No API keys, passwords, or user PII in log messages
- [ ] Error logs include context (userId, operation) but not sensitive request bodies

## Step 11 — Configuration & Deployment Analysis

- [ ] `application.properties` exists with all required keys
- [ ] Database credentials externalized (environment variables, not hardcoded)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (not `create`, `update`, or `create-drop`)
- [ ] Flyway or Liquibase configured for schema migrations
- [ ] Health check endpoint enabled (`spring.actuator`)
- [ ] Connection pool tuned (`spring.datasource.hikari.*`)
- [ ] WebSocket STOMP broker configured with appropriate heartbeat intervals

## Step 12 — Risk Assessment

Classify each finding by severity:

### Risk Categories

**[P0 — Breaking]** — Will break production or corrupt financial data:
- Price data corruption: wrong ticker-price association, duplicate records
- Portfolio value miscalculation: incorrect holdings, wrong cash balance
- Transaction integrity: missing audit trail, inconsistent buy/sell operations
- Entity relationship corruption: orphaned records, broken foreign keys
- WebSocket pipeline break: trades not persisted or not broadcast
- Authentication bypass: access to other users' portfolios

**[P1 — High Risk]** — Likely to cause incidents under load:
- Thread safety violation: shared mutable state in singleton beans
- Database overwhelm: unbatched writes at Finnhub's message rate
- Memory leak: unbounded in-memory collections, unclosed resources
- Connection pool exhaustion: long-held connections, missing timeouts
- N+1 query patterns in hot paths (price lookup, portfolio aggregation)
- WebSocket disconnect without reconnection

**[P2 — Medium Risk]** — May cause issues over time:
- Missing `@Transactional` on multi-entity operations
- Missing validation on REST endpoints
- Incomplete error handling on non-critical paths
- Missing indexes on frequently queried columns
- Insufficient test coverage for changed functionality
- Inconsistent timestamp types across entities

**[P3 — Low Risk]** — Quality issues, not production-impacting:
- Convention drift (naming, missing annotations, Lombok misuse)
- Verbose code that could be simplified
- Missing Javadoc on public API methods
- Redundant annotations or imports

## Step 13 — Produce the Verdict

Output this structure:

```markdown
## Code Review: {scope description}

### Verdict: {APPROVED | NEEDS FIX | NEEDS DISCUSSION}

---

### Regression Risk
**Pipeline Affected**: {Streaming | REST API | JPA Entities | Portfolio Logic | None}
{Assessment of whether changes break the streaming pipeline or data flow.}

### Data Integrity
**Status**: {PASS | FAIL | N/A}
{Assessment of financial data accuracy — price records, portfolio values, transaction consistency.}

### Accuracy
**Logic Correctness**: {Assessment of null safety, numeric accuracy, DTO mapping.}
**External API**: {Assessment of Finnhub integration correctness.}

### Robustness
**Error Handling**: {Assessment of exception handling, graceful degradation.}
**Resource Management**: {Assessment of connection pooling, memory usage, cleanup.}

### Performance
**Thread Safety**: {Assessment of concurrent access patterns.}
**Database**: {Assessment of query efficiency, batching, indexing.}

### Type Safety
**Java 21 / Spring Boot**: {Assessment of modern conventions, Lombok usage, DI patterns.}

### Testing
{Assessment of test existence, coverage, and quality for changed code.}

### Security & Observability
{Assessment of secret handling, input validation, logging.}

---

### Findings
1. [{TAG}] {Description} — `{file:line}`
2. [{TAG}] {Description} — `{file:line}`
...

### Risk Summary
- P0: {count}
- P1: {count}
- P2: {count}
- P3: {count}

### Recommendations
{Prioritized action items.}
```

## Verdict Rules

- Any **[BUG]**, **[DATA]**, **[SEC]**, or **[P0]** finding -> **NEEDS FIX**
- Any **[REGRESSION]** finding -> **NEEDS FIX**
- Multiple **[PERF]**, **[ENTITY]**, **[STREAM]**, **[TYPE]**, or **[TEST]** findings -> **NEEDS FIX**
- **[P1]** risks without mitigation -> **NEEDS FIX**
- Architectural decisions needed -> **NEEDS DISCUSSION**
- Only **[CONV]**, **[NOTE]**, **[P3]** items -> **APPROVED**
- Clean code -> **APPROVED** with brief confirmation

**Do NOT rubber-stamp.** Read every line of the diff. If it's clean, say so briefly.
