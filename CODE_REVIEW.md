# AssetFlow — Code Analysis & Improvement Guide

**Date**: 2026-03-28 | **Scope**: Full repository

---

## Overview

Spring Boot 4 / Java 21 portfolio management app with real-time Finnhub market data streaming. The streaming pipeline is the most complete part. Portfolio, identity, and REST APIs are scaffolded but largely unimplemented.

### Architecture

```
Finnhub WebSocket (wss://ws.finnhub.io)
  -> FinnhubWebSocketHandler.handleTextMessage()    [parses JSON trade messages]
  -> MarketDataService.processTrades()               [saves to DB + broadcasts]
  -> priceRepository.save()                          [PostgreSQL via JPA]
  -> SimpMessagingTemplate.convertAndSend()           [STOMP to frontend via SockJS]
```

### Tech Stack
- **Backend**: Spring Boot 4.0.3, Java 21, Maven
- **Database**: PostgreSQL (via Spring Data JPA / Hibernate)
- **Real-time**: Finnhub WebSocket (inbound) + STOMP over SockJS (outbound to frontend)
- **Utilities**: Lombok, Jackson 2.x

---

## 1. Application Configuration

### What's wrong

There is no `src/main/resources/` directory and no `application.properties` file. Spring Boot uses this file as the central place to configure everything — database connection, external API keys, Hibernate behavior, server port, etc. Without it, the app throws an exception on startup before any code runs.

The codebase references several properties that don't exist anywhere:
- `FinnhubWebSocketConfig.java:17` uses `@Value("${app.finnhub.api-key}")`
- `FinnhubWebSocketConfig.java:20` uses `@Value("${app.finnhub.base-url}")`
- `SubscribeData.java` uses `@ConfigurationProperties(prefix = "app.finnhub")`
- Spring Data JPA expects `spring.datasource.*` to know where PostgreSQL lives

### How to do it properly

Create `src/main/resources/application.properties`:

```properties
# ── Server ──
server.port=8080

# ── Database ──
spring.datasource.url=jdbc:postgresql://localhost:5432/assetflow
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# ── Hibernate ──
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true

# ── Connection Pool (HikariCP) ──
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000

# ── Finnhub ──
app.finnhub.api-key=${FINNHUB_API_KEY}
app.finnhub.base-url=wss://ws.finnhub.io
app.finnhub.tickers=AAPL,MSFT,TSLA,VOO,NVDA
```

Key points:
- **Never hardcode credentials.** Use `${ENV_VAR}` placeholders so secrets come from the environment, not source code.
- **Always set `ddl-auto=validate`** in anything beyond local dev. This tells Hibernate to verify the schema matches your entities but never modify it. Use Flyway or Liquibase to manage schema changes as versioned SQL scripts.
- **Tune HikariCP.** The default pool size is 10. With concurrent WebSocket writes and REST reads, you'll need more. But don't set it too high — each connection holds a PostgreSQL backend process.
- **Enable batch inserts.** `batch_size=50` + `order_inserts=true` lets Hibernate group multiple `INSERT` statements into a single round-trip, which is critical for high-frequency price data.

Also add to `.gitignore`:
```
application-local.properties
.env
*.key
```

---

## 2. Jackson Import Mismatch

### What's wrong

`FinnhubWebSocketHandler.java:11`:
```java
import tools.jackson.databind.ObjectMapper;  // Jackson 3.x namespace
```

All DTOs use `com.fasterxml.jackson` annotations:
```java
// FinnHubTrade.java
import com.fasterxml.jackson.annotation.JsonProperty;  // Jackson 2.x namespace
```

Jackson 2.x (`com.fasterxml.jackson`) and Jackson 3.x (`tools.jackson`) are completely separate libraries with different package names. Spring Boot 4.0.3 ships Jackson 2.x. The `ObjectMapper` from Jackson 3.x doesn't recognize Jackson 2.x annotations like `@JsonProperty`, so serialization and deserialization silently produce wrong results or fail entirely.

### How to fix

```java
import com.fasterxml.jackson.databind.ObjectMapper;  // matches the DTOs
```

Spring Boot auto-configures an `ObjectMapper` bean with sensible defaults. The handler already receives it via constructor injection, so it will use Spring's pre-configured instance — no manual setup needed.

---

## 3. WebSocket Topic Path

### What's wrong

`MarketDataService.java:42`:
```java
messagingTemplate.convertAndSend("/topic/market" + trade.ticker(), ...);
// Produces: /topic/marketAAPL
```

The topic name becomes `/topic/marketAAPL` — a single string with no separator. A frontend subscribing to `/topic/market/AAPL` receives nothing because STOMP treats these as completely different destinations.

### How to fix

```java
messagingTemplate.convertAndSend("/topic/market/" + trade.ticker(), ...);
// Produces: /topic/market/AAPL
```

Using `/` as a separator is also a STOMP convention — it creates a hierarchy where you could subscribe to `/topic/market/*` in the future if the broker supports wildcards.

---

## 4. Dependency Injection in PortfolioService

### What's wrong

`PortfolioService.java:26-27`:
```java
@Service
public class PortfolioService {
    private PortfolioRepository portfolioRepository;
    private HoldingRepository holdingRepository;
    // No constructor, no @Autowired, no @RequiredArgsConstructor
```

Spring creates this bean using the default no-arg constructor, leaving both fields as `null`. Every method that touches the repositories (`getPortfolioById`, `getHoldingById`, `updateCashBalance`) throws `NullPointerException`.

In Spring, there are three ways to inject dependencies: field injection (`@Autowired` on the field), setter injection, and constructor injection. **Constructor injection is the recommended approach** because:
1. Fields can be `final` — the object is fully initialized at construction time
2. You can't accidentally create an instance without its dependencies
3. It makes testing easy — just pass mocks via the constructor

### How to do it properly

```java
@Service
@RequiredArgsConstructor  // Lombok generates the constructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
```

`@RequiredArgsConstructor` generates a constructor with all `final` fields as parameters. Spring sees a single constructor and auto-injects the matching beans. No `@Autowired` needed.

The same pattern should be applied everywhere. Compare with `MarketDataService.java` which already does it correctly with an explicit constructor.

---

## 5. JPA Enum Mapping

### What's wrong

`Asset.java:29` and `Transaction.java:43`:
```java
@Column(name="asset_type")
private AssetType assetType;  // no @Enumerated annotation
```

Without `@Enumerated(EnumType.STRING)`, JPA uses `EnumType.ORDINAL` by default. This stores the enum's **position number** — `STOCK` = 0, `ETF` = 1. The problem: if you later add `BOND` between them or reorder the enum, every existing row in the database silently maps to the wrong value. There's no error, no exception — just corrupt data.

This is one of the most common JPA mistakes because it works perfectly in development but breaks silently months later when someone modifies the enum.

### How to do it properly

```java
@Enumerated(EnumType.STRING)
@Column(name="asset_type")
private AssetType assetType;  // stored as "STOCK", "ETF" in the database
```

`EnumType.STRING` stores the enum's **name** as text. You can freely reorder enums, add new values, or remove unused ones without affecting existing data. The only thing you can't do is rename a value — but that's a much rarer and more deliberate change.

Apply this to both `Asset.assetType` and `Transaction.type`.

---

## 6. JPA Entity Relationships — @OneToOne Ownership

### What's wrong

`User.java:29`:
```java
@OneToOne
@JoinColumn(name = "portfolio_id")
private Portfolio portfolio;
```

`Portfolio.java:26`:
```java
@OneToOne
@JoinColumn(name = "user_id")
private User user;
```

Both sides declare `@JoinColumn`, which means both sides "own" the relationship. Hibernate creates two foreign key columns: `users.portfolio_id` AND `portfolios.user_id`. They can go out of sync — you could update one without updating the other, leading to broken data.

In JPA, every bidirectional relationship has an **owning side** (the one with the foreign key column) and an **inverse side** (the one that mirrors it using `mappedBy`). Only the owning side's changes are persisted to the database.

### How to do it properly

Pick `Portfolio` as the owner (it stores `user_id`), and make `User` the inverse side:

```java
// Portfolio.java — OWNER (has the actual foreign key column)
@OneToOne
@JoinColumn(name = "user_id")
private User user;

// User.java — INVERSE (points back via mappedBy, no extra column)
@OneToOne(mappedBy = "user")
private Portfolio portfolio;
```

Now there's only one foreign key column (`portfolios.user_id`), and Hibernate keeps both sides in sync automatically.

Also, `Portfolio.java:35` has a confusing transient field:
```java
@Transient
private List<Asset> portfolio;  // dead code — never populated
```

This should either become a proper relationship to `Holding` or be deleted:
```java
@OneToMany(mappedBy = "portfolio")
private List<Holding> holdings;
```

---

## 7. Lombok @Data on JPA Entities

### What's wrong

All 8 entity classes (`User`, `Asset`, `Portfolio`, `Holding`, `Price`, `Transaction`, `ExchangeRate`, `Currency`) use `@Data`.

Lombok's `@Data` generates `equals()`, `hashCode()`, and `toString()` using **every field** in the class. For JPA entities, this causes three problems:

1. **Lazy-loading triggers**: `equals()` accessing a `@ManyToOne` field forces Hibernate to load that association from the database. If you're outside a transaction (e.g., in a controller), this throws `LazyInitializationException`.

2. **Circular `toString()`**: `User` has `Portfolio`, `Portfolio` has `User`. Calling `toString()` on either creates an infinite loop → `StackOverflowError`.

3. **Broken Set/Map behavior**: If you add an entity to a `HashSet` before it has an ID (pre-persist), then persist it (ID gets assigned), the hash code changes and the Set can no longer find the entity.

### How to do it properly

Replace `@Data` with `@Getter @Setter` and implement `equals()`/`hashCode()` manually based on the entity ID:

```java
@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... fields ...

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();  // constant — safe for Sets across persist
    }
}
```

Why `getClass().hashCode()` for `hashCode()`? Because the ID is `null` before persist. A constant hash code means the entity stays findable in a `HashSet` even after its ID changes. The tradeoff is that all entities of the same type land in the same hash bucket — but in practice, entity Sets are small and this is fine.

---

## 8. Database Write Performance — Streaming Pipeline

### What's wrong

`MarketDataService.java:30-46`:
```java
public void processTrades(FinnHubTrade[] trades) {
    Arrays.stream(trades).forEach(trade -> {
        Asset asset = assetRepository.findByTicker(trade.ticker());  // 1 SELECT per trade
        if (asset != null) {
            Price price = new Price();
            // ... set fields ...
            priceRepository.save(price);  // 1 INSERT per trade
            messagingTemplate.convertAndSend(...);
        }
    });
}
```

Three performance problems:

1. **N+1 queries**: For an array of 100 trades across 5 tickers, this runs 100 `SELECT` queries to look up the same 5 assets repeatedly.
2. **Individual inserts**: 100 separate `INSERT` statements instead of one batched insert.
3. **No transaction boundary**: Each `save()` auto-commits in its own transaction. 100 trades = 100 transaction open/commit cycles. If it fails at trade #50, the first 49 are saved and the rest are lost.

Finnhub can send hundreds of messages per second during market hours. This code creates 2x that number in database queries.

### How to do it properly

```java
@Transactional
public void processTrades(FinnHubTrade[] trades) {
    // 1. Cache asset lookups — each ticker hits the DB only once
    Map<String, Asset> assetCache = Arrays.stream(trades)
            .map(FinnHubTrade::ticker)
            .distinct()
            .map(ticker -> Map.entry(ticker, assetRepository.findByTicker(ticker)))
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // 2. Build all Price entities in memory
    List<Price> prices = Arrays.stream(trades)
            .filter(trade -> assetCache.containsKey(trade.ticker()))
            .map(trade -> {
                Price price = new Price();
                price.setAsset(assetCache.get(trade.ticker()));
                price.setPrice(trade.price());
                price.setRecordedAt(trade.timestamp());
                return price;
            })
            .toList();

    // 3. Batch insert — one round-trip to PostgreSQL
    priceRepository.saveAll(prices);

    // 4. Broadcast to frontend
    trades.forEach(trade -> {
        Asset asset = assetCache.get(trade.ticker());
        if (asset != null) {
            messagingTemplate.convertAndSend(
                "/topic/market/" + trade.ticker(),
                new MarketUpdateDTO(trade.ticker(), trade.price()));
        }
    });
}
```

Key improvements:
- **Asset cache**: Each ticker is looked up once, regardless of how many trades reference it.
- **`saveAll()`**: Hibernate batches these into fewer SQL statements (controlled by `hibernate.jdbc.batch_size`).
- **`@Transactional`**: All inserts succeed or fail together. No partial writes.
- For even better performance, consider buffering trades over a time window (e.g., 5 seconds) and persisting snapshots instead of every tick.

---

## 9. Null Safety in WebSocket Handler

### What's wrong

`FinnhubWebSocketHandler.java:47-48`:
```java
if("trade".equals(response.type()) && response.type() != null) {
    marketDataService.processTrades(response.data());
}
```

Two issues:
1. The `response.type() != null` check comes AFTER `"trade".equals(response.type())`. The `equals()` already handles null safely (returns `false`), so the check is redundant but harmless.
2. `response.data()` is **never checked**. If Finnhub sends `{"type":"trade","data":null}`, the code passes `null` into `processTrades()`, which calls `Arrays.stream(null)` → `NullPointerException`.

### How to fix

```java
if ("trade".equals(response.type()) && response.data() != null) {
    marketDataService.processTrades(response.data());
    log.debug("Processed {} trades", response.data().length);
}
```

---

## 10. Long Comparison with ==

### What's wrong

`PortfolioService.java:74`:
```java
.filter(transaction -> transaction.getId() == id)
```

Java's `Long` is an object wrapper around `long`. Java caches `Long` instances for values -128 to 127 — so for small numbers, `==` works because both variables point to the same cached object. But for any value above 127 (which includes almost every database-generated ID), Java creates separate `Long` objects. `==` compares **object references**, not values, so two `Long` objects with the same value return `false`.

This is a classic Java pitfall that passes all tests with small IDs but fails in production.

### How to fix

```java
.filter(transaction -> transaction.getId().equals(id))
```

Always use `.equals()` for wrapper types (`Long`, `Integer`, `Double`). Or use primitive `long` when null isn't a valid value.

---

## 11. Incomplete Spring Beans — UserRepository, UserService, UserResource

### What's wrong

```java
// UserRepository.java
public interface UserRepository {}  // doesn't extend JpaRepository

// UserService.java
public class UserService {}  // missing @Service

// UserResource.java
public class UserResource {}  // missing @RestController
```

Spring only manages classes it knows about. `@Service` tells Spring to create a singleton bean. `@RestController` tells Spring to map HTTP requests to methods. `JpaRepository<User, Long>` tells Spring Data to generate CRUD implementations. Without these, the classes are just regular Java classes — invisible to the framework.

### How to do it properly

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserResource {
    private final UserService userService;
}
```

---

## 12. WebSocket Reconnection

### What's wrong

`FinnhubWebSocketHandler.java` extends `TextWebSocketHandler` but only overrides `afterConnectionEstablished()` and `handleTextMessage()`. There's no `handleTransportError()` and no `afterConnectionClosed()`.

If the Finnhub WebSocket disconnects — network blip, rate limit, server restart — the connection closes silently. The app continues running but receives zero market data. There's no log message, no alert, and no reconnection attempt.

### How to do it properly

Override the error and close callbacks, and trigger a reconnect with backoff:

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.error("Finnhub WebSocket error", exception);
    scheduleReconnect();
}

@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    log.warn("Finnhub WebSocket closed: {}", status);
    scheduleReconnect();
}

private void scheduleReconnect() {
    // Use a ScheduledExecutorService with exponential backoff
    // Call webSocketConnectionManager.start() to re-establish
}
```

For production, consider using Spring's `WebSocketConnectionManager` with `setAutoStartup(true)` combined with a health check that monitors the connection state.

---

## 13. Logging

### What's wrong

`MarketDataService.java:31` and `FinnhubWebSocketHandler.java:49` use `System.out.println`.

`System.out` is:
- **Unbuffered** — each call flushes immediately, which is slow under high throughput
- **Unconfigurable** — you can't set log levels, filter by package, or route to files
- **Missing context** — no timestamps, no thread names, no class names
- **Unfilterable** — you can't turn off debug logs in production without changing code

At Finnhub's message rate (hundreds of messages/second), `System.out.println` will flood stdout and slow down the application.

### How to do it properly

```java
@Slf4j  // Lombok generates: private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
@Service
public class MarketDataService {

    public void processTrades(FinnHubTrade[] trades) {
        log.debug("Processing {} trades", trades.length);  // only appears at DEBUG level
        // ...
        log.info("Saved {} prices for ticker {}", prices.size(), ticker);  // appears at INFO+
    }
}
```

With SLF4J + Logback (Spring Boot's default), you can control log levels per package in `application.properties`:
```properties
logging.level.com.project3.AssetFlow.market=DEBUG
logging.level.com.project3.AssetFlow.streaming=INFO
```

---

## 14. Timestamp Consistency

### What's wrong

| Entity | Field | Type | What it stores |
|---|---|---|---|
| `Price` | `recordedAt` | `Long` | Epoch milliseconds from Finnhub |
| `ExchangeRate` | `recordedAt` | `LocalDate` | Date only, no time |
| `Transaction` | `executionTime` | `LocalDateTime` | Date + time, no timezone |
| `User` | `createdAt` | `LocalDateTime` | Date + time, no timezone |

Three different time representations in one app. If you need to query "all prices and transactions in the last hour," you'd need to convert between epoch millis and `LocalDateTime` — which requires knowing the timezone, which isn't stored anywhere.

### How to do it properly

Use `Instant` for all machine-generated timestamps. `Instant` represents a point on the UTC timeline and maps cleanly to PostgreSQL's `TIMESTAMP WITH TIME ZONE`.

```java
// Price.java
@Column(name = "recorded_at")
private Instant recordedAt;

// In MarketDataService, convert Finnhub's epoch millis:
price.setRecordedAt(Instant.ofEpochMilli(trade.timestamp()));
```

Use `LocalDate` only when you genuinely mean "a calendar date with no time" (e.g., exchange rate effective date). Use `Instant` for everything else.

---

## 15. CORS Configuration

### What's wrong

No CORS headers are configured anywhere. If the frontend runs on `localhost:3000` and the backend on `localhost:8080`, the browser blocks all cross-origin requests — including the SockJS WebSocket handshake. You'll see `CORS policy: No 'Access-Control-Allow-Origin' header` in the browser console.

### How to do it properly

Add allowed origins to the STOMP endpoint in `FrontendWebSocketConfig.java`:
```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-market")
            .setAllowedOrigins("http://localhost:3000")  // frontend origin
            .withSockJS();
}
```

For REST endpoints, add a global CORS configuration:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

Never use `"*"` for `allowedOrigins` in production — it allows any website to make requests to your API.

---

## 16. Password Security

### What's wrong

`User.java:27`:
```java
@Column(nullable = false)
private String password;  // stored as plain text
```

Spring Security is commented out in `pom.xml`. Passwords are stored exactly as the user typed them. If the database is breached, every password is immediately readable.

### How to do it properly

1. Re-enable `spring-boot-starter-security` in `pom.xml`
2. Hash passwords with BCrypt before saving:
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String email, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));  // hashed
        return userRepository.save(user);
    }
}
```

BCrypt is a one-way hash — you can verify a password against it, but you can never reverse it back to plaintext.

---

## 17. PortfolioResource — Redundant Injection

### What's wrong

`PortfolioResource.java:14`:
```java
@RestController
@AllArgsConstructor
public class PortfolioResource {
    @Autowired
    private final PortfolioService portfolioService;
}
```

`@AllArgsConstructor` generates a constructor with all fields. Spring sees that constructor and injects `PortfolioService` through it — this is **constructor injection**. At the same time, `@Autowired` on the field tells Spring to also inject via **field reflection** after construction. Having both is contradictory.

### How to fix

Pick one. Constructor injection (via `@AllArgsConstructor` or `@RequiredArgsConstructor`) is preferred:
```java
@RestController
@RequiredArgsConstructor
public class PortfolioResource {
    private final PortfolioService portfolioService;
}
```

---

## 18. Static Data Mixed with JPA

### What's wrong

`PortfolioService.java:13-23`:
```java
@Service
public class PortfolioService {
    private static List<Asset> portfolio = new ArrayList<>();
    static {
        portfolio.addAll(List.of(
            new Asset("AAPL", "Apple Inc.", "USA", AssetType.STOCK),
            // ... more hardcoded assets
        ));
    }

    public List<Asset> getAllAssets() { return portfolio; }           // returns static data
    public Portfolio getPortfolioById(Long id) { return portfolioRepository.findById(id)... }  // queries DB
```

Two data sources in one service. `getAllAssets()` returns hardcoded data that never updates. `getPortfolioById()` reads from the database. A caller has no way to know which methods use real data.

### How to fix

Remove the static list. Seed the database using Flyway migration scripts or a `CommandLineRunner`, then always query via `AssetRepository`:
```java
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }
}
```

---

## 19. AssetRepository Return Type

### What's wrong

`AssetRepository.java:8`:
```java
Asset findByTicker(String ticker);  // returns null if not found
```

Returning a raw type means the "might not exist" case is invisible at the type level. `MarketDataService` does null-check, but there's nothing stopping a future caller from writing `assetRepository.findByTicker("XYZ").getAssetName()` — which NPEs when the ticker doesn't exist.

### How to do it properly

```java
Optional<Asset> findByTicker(String ticker);
```

`Optional` forces the caller to explicitly handle the absent case:
```java
Asset asset = assetRepository.findByTicker(trade.ticker())
        .orElseThrow(() -> new AssetNotFoundException(trade.ticker()));
```

---

## 20. Test Coverage

### What's wrong

The only test:
```java
@SpringBootTest
class TransactionFlowApplicationTests {
    @Test
    void contextLoads() {}
}
```

This single test will fail because there's no database configuration. Beyond that, there are zero tests for any business logic — `MarketDataService`, `PortfolioService`, the WebSocket handler, entity mappings, or DTOs.

### What to test first

1. **`MarketDataService.processTrades()`** — the core pipeline. Mock the repositories, verify that prices are saved and messages are broadcast.
2. **`PortfolioService.updateCashBalance()`** — financial logic. Test positive balance, zero balance, insufficient funds.
3. **`FinnhubWebSocketHandler.handleTextMessage()`** — JSON parsing. Test valid trade messages, null data, unknown message types.
4. **Entity mappings** — use `@DataJpaTest` with an embedded database to verify JPA annotations work correctly.

Example unit test:
```java
@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock private PriceRepository priceRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private MarketDataService service;

    @Test
    void processTrades_savesPrice_andBroadcasts() {
        Asset apple = new Asset("AAPL", "Apple", "USA", AssetType.STOCK);
        when(assetRepository.findByTicker("AAPL")).thenReturn(apple);

        FinnHubTrade trade = new FinnHubTrade("AAPL", new BigDecimal("150.00"), 1234567890L);
        service.processTrades(new FinnHubTrade[]{trade});

        verify(priceRepository).save(any(Price.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/market/AAPL"), any(MarketUpdateDTO.class));
    }

    @Test
    void processTrades_unknownTicker_skipsGracefully() {
        when(assetRepository.findByTicker("UNKNOWN")).thenReturn(null);

        FinnHubTrade trade = new FinnHubTrade("UNKNOWN", new BigDecimal("10.00"), 1234567890L);
        service.processTrades(new FinnHubTrade[]{trade});

        verify(priceRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MarketUpdateDTO.class));
    }
}
```

---

## Database Connection — Best Practices Summary

| Practice | Status | Why it matters |
|---|---|---|
| Config file exists | Missing | App can't connect to any database without it |
| Credentials via env vars | N/A | Hardcoded creds get committed to git and leaked |
| `ddl-auto=validate` | Not set | `create`/`update` can drop columns or tables in prod |
| Flyway migrations | Missing | Schema changes need versioned, reviewable SQL scripts |
| HikariCP tuning | Default | Default pool (10) may be too small for WebSocket + REST load |
| `readOnly` transactions | Missing | Read-only hints skip dirty checks, improving query perf |
| Batch inserts | Missing | Individual inserts at Finnhub's rate will overwhelm PostgreSQL |
| Pool sizing | Default (10) | Concurrent WebSocket writes + REST reads need ~20-30 connections |
| Index strategy | Unknown | `ticker`, `user_id`, `portfolio_id` need indexes for fast lookups |

---

## Summary

| Severity | Count | Description |
|---|---|---|
| Breaking | 6 | App won't start or will corrupt data |
| High Risk | 8 | Failures or severe perf issues under real load |
| Medium Risk | 5 | Problems as the project grows |
| Low Risk | 5 | Code quality cleanup |
| **Total** | **24** | |

---

## Fix Priority

1. Create `application.properties` with DB, Finnhub, and Hibernate config
2. Fix Jackson import (`tools.jackson` -> `com.fasterxml.jackson`)
3. Fix WebSocket topic path (add `/`)
4. Add constructor injection to `PortfolioService`
5. Add `@Enumerated(EnumType.STRING)` on all enum fields
6. Fix `@OneToOne` ownership with `mappedBy`
7. Replace `@Data` with `@Getter @Setter` on entities
8. Add Flyway for schema migrations
9. Batch or throttle `processTrades` writes
10. Add CORS config, SLF4J logging, and WebSocket reconnection
