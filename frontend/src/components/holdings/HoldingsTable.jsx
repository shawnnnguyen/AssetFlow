import HoldingRow from './HoldingRow';

export default function HoldingsTable({ holdings = [], livePrices, dayChangePct, lastUpdated }) {
  return (
    <div className="panel">
      <div className="phead">
        <div>
          <h2>Holdings</h2>
          <div className="meta">{holdings.length} positions{lastUpdated ? ` · ${lastUpdated}` : ''}</div>
        </div>
      </div>

      <div className="holdings-grid">
        <div className="gh">Asset</div>
        <div className="gh r">Qty</div>
        <div className="gh r">Live price</div>
        <div className="gh r">Avg cost</div>
        <div className="gh r">Market value</div>
        <div className="gh r">Session %</div>
        <div className="gh" />
      </div>

      {holdings.length === 0 ? (
        <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>
          No holdings yet. Record a transaction to get started.
        </div>
      ) : (
        holdings.map(h => (
          <HoldingRow
            key={h.holdingId ?? h.id}
            holding={h}
            livePrices={livePrices}
            dayChangePct={dayChangePct}
          />
        ))
      )}
    </div>
  );
}
