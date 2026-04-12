# RFC: Decouple Stock Ingestion from Client Serving

## Problem

Right now, everything happens in one place. When a Finnhub price tick arrives, `MarketDataService.processTrades()` does all of this in a single method:

1. Updates the in-memory price cache
2. Saves the price to the database
3. Broadcasts the price to frontend via STOMP
4. Publishes a Spring event that triggers portfolio recalculation for *every* portfolio holding that asset

The ingestion side is aware of the client-serving side. If we change how we push data to clients, we have to touch the ingestion code. The portfolio recalculation runs for all affected portfolios every second, even if no one is watching.

## Proposed Change

Split the system into two independent responsibilities:

- **Ingestion**: receives stock data, stores it. Done.
- **Client-serving**: knows who's subscribed, polls for prices, calculates, pushes.

They share the in-memory price cache. That's the only link between them.

## Stock Data Lifecycle

### Current

```
Finnhub tick
  │
  ▼
MarketDataService.processTrades()
  ├── update liveCache
  ├── save Price to DB
  ├── push to frontend via STOMP        ← ingestion doing client work
  └── publish PriceUpdateEvent
        │
        ▼
      PortfolioStreamingService
        ├── find ALL portfolios holding this asset
        ├── add to pendingPortfolioIds
        └── @Scheduled(1s): recalculate ALL pending portfolios
              └── push to user via STOMP (even if no one is listening)
```

### Proposed

```
Finnhub tick
  │
  ▼
MarketDataService.processTrades()
  ├── update liveCache
  └── save Price to DB
      (done. ingestion has no idea clients exist.)


                    ~~~~ completely independent ~~~~


Client connects via WebSocket
  │
  ▼
SubscriptionManager
  ├── tracks: userId → portfolioId → tickers
  └── maintains aggregated ticker set (union of all subscribed tickers)

ClientServingService (@Scheduled, every 1s)
  │
  ├── 1. Poll prices from liveCache for aggregated tickers
  │      (no DB hit, just a map read)
  │
  ├── 2. For each subscriber, compute portfolio performance
  │      using the polled prices
  │
  └── 3. Push result to that subscriber via WebSocket
```

## Key Differences

| | Current | Proposed |
|---|---|---|
| Ingestion knows about clients? | Yes — pushes to STOMP, publishes events | No — writes cache + DB, nothing else |
| Portfolio recalc triggered by | Every price tick, for all holders | Scheduled poll, only for subscribers |
| Work at 0 viewers | Still queries DB, still computes | Zero |
| Work scales with | Total portfolios holding any active asset | Number of active WebSocket subscribers |

## The Polling Trick

The serving side doesn't need to detect DB writes or use fancy change-detection. The ingestion side already updates `liveCache` (a ConcurrentHashMap) on every tick. The serving side just reads from it.

```
Every 1 second:
  tickers = union of all subscribed clients' tickers
  prices = liveCache.get(ticker) for each ticker    ← in-memory, ~free
  for each subscriber:
    calculate + push
```

This works because both sides live in the same JVM. The cache *is* the notification mechanism. No LISTEN/NOTIFY, no CDC, no message queue. Just a shared map.

If the ingestion were ever extracted into a separate service, we'd revisit this (Postgres LISTEN/NOTIFY or a message broker would replace the shared cache). But for a single-process Spring Boot app, this is the simplest thing that works.

## Note: Separate Price Polling from Portfolio Calculation

These are two distinct steps and should stay that way:

1. **Price polling** — read the latest prices from `liveCache` for the aggregated ticker set. This is a bulk operation done once per cycle. It produces a `Map<assetId, price>`.

2. **Portfolio calculation** — for each subscriber, take their holdings and compute performance against the price map from step 1.

Don't mix them. The price snapshot is taken once per cycle and shared across all portfolio calculations in that cycle. This means:
- All subscribers see a consistent price snapshot (no subscriber seeing AAPL at $185 while another sees $186 from a tick that arrived mid-loop)
- Price reading is O(tickers), not O(subscribers * tickers)
- If we ever need to swap out the price source (e.g., move to Redis, external service), we only change step 1

## Subscription Lifecycle

```
Client opens portfolio page
  → STOMP SUBSCRIBE /user/queue/portfolio/{portfolioId}
  → Server receives SessionSubscribeEvent
  → SubscriptionManager registers: userId, portfolioId, resolve tickers from holdings
  → Client starts receiving updates

Client leaves portfolio page (or disconnects)
  → STOMP UNSUBSCRIBE (or SessionDisconnectEvent)
  → SubscriptionManager deregisters
  → No more computation for that portfolio
```

## What Gets Removed

- `PriceUpdateEvent` and all event publishing from `MarketDataService`
- `PortfolioStreamingService.handleAssetPriceUpdate()` (the @EventListener)
- The blind `findPortfoliosByAssetIdWithUser` query that fires on every tick

## What Gets Added

- `SubscriptionManager` — tracks active WebSocket subscriptions, maintains aggregated ticker set
- `ClientServingService` — scheduled service that polls cache, calculates, pushes (replaces `PortfolioStreamingService`)
- SessionSubscribeEvent / SessionDisconnectEvent listeners to manage subscription lifecycle
