import { useState, useEffect, useRef } from 'react';

export default function HoldingRow({ holding, livePrices }) {
  const livePrice = livePrices?.get(holding.ticker) ?? holding.currentMarketPrice ?? 0;
  const avgCost   = holding.avgCost ?? 0;
  const pnlPct    = avgCost > 0 ? ((livePrice - avgCost) / avgCost) * 100 : 0;
  const pos       = pnlPct >= 0;

  const prevPriceRef = useRef(livePrice);
  const [flash, setFlash] = useState('');

  useEffect(() => {
    if (livePrice === prevPriceRef.current) return;
    const cls = livePrice > prevPriceRef.current ? 'flash-pos' : 'flash-neg';
    prevPriceRef.current = livePrice;
    setFlash(cls);
    const t = setTimeout(() => setFlash(''), 600);
    return () => clearTimeout(t);
  }, [livePrice]);

  const cellBg = flash
    ? { background: flash === 'flash-pos' ? 'var(--pos-soft)' : 'var(--neg-soft)', transition: 'background var(--dur) var(--ease)' }
    : { transition: 'background var(--dur) var(--ease)' };

  return (
    <div style={{ display: 'contents' }}>
      <div className="gc" style={cellBg}>
        <div className="sym">{holding.ticker ?? '—'}</div>
        {holding.companyName && <div className="holding-name">{holding.companyName}</div>}
      </div>
      <div className="gc r num" style={cellBg}>
        {holding.quantity?.toLocaleString() ?? '—'}
      </div>
      <div className="gc r num" style={cellBg}>
        <div>${avgCost.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
        <div className={`holding-name ${pos ? 'pos' : 'neg'}`} style={{ marginTop: 2 }}>
          {pos ? '+' : ''}{pnlPct.toFixed(2)}%
        </div>
      </div>
    </div>
  );
}
