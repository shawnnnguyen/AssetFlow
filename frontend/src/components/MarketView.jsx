export default function MarketView({ trackedStocks = [], livePrices, dayChangePct, lastUpdatedAt }) {
  return (
    <div className="panel">
      <div className="phead">
        <div>
          <h2>Market</h2>
          <div className="meta">
            {trackedStocks.length} tracked
            {lastUpdatedAt ? ` · ${lastUpdatedAt.toLocaleTimeString()}` : ' · connecting…'}
          </div>
        </div>
      </div>

      <div className="market-grid">
        <div className="gh">Ticker</div>
        <div className="gh r">Live price</div>
        <div className="gh r">Session %</div>
      </div>

      {trackedStocks.length === 0 ? (
        <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>
          No tracked tickers. Use Find Ticker to start tracking.
        </div>
      ) : (
        trackedStocks.map(s => {
          const price = livePrices?.get(s.ticker) ?? Number(s.latestPrice ?? 0);
          const pct   = dayChangePct?.get(s.ticker) ?? 0;
          const pos   = pct >= 0;
          return (
            <div key={s.assetId} className="market-grid" style={{ display: 'contents' }}>
              <div className="gc"><div className="sym">{s.ticker}</div></div>
              <div className="gc r num">
                ${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
              <div className={`gc r num ${pos ? 'pos' : 'neg'}`}>
                {pos ? '+' : ''}{pct.toFixed(2)}%
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}
