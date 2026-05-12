import { useState, useEffect, useRef } from 'react';
import { ChevronIcon } from '../shared/Icons';

export default function HoldingRow({ holding, livePrices, dayChangePct }) {
  const livePrice  = livePrices?.get(holding.ticker) ?? holding.currentMarketPrice ?? 0;
  const liveValue  = livePrice * (holding.quantity ?? 0);
  const sessionPct = dayChangePct?.get(holding.ticker) ?? holding.percentageChange ?? 0;
  const pos        = sessionPct >= 0;

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

  const rowStyle = flash ? { background: flash === 'flash-pos' ? 'var(--pos-soft)' : 'var(--neg-soft)', transition: 'background var(--dur) var(--ease)' } : { transition: 'background var(--dur) var(--ease)' };

  return (
    <div className="holdings-grid" style={{ display: 'contents' }}>
      <div className="gc" style={rowStyle}>
        <div className="sym">{holding.ticker ?? '—'}</div>
        <div className="holding-name">{holding.companyName ?? ''}</div>
      </div>
      <div className="gc r num" style={rowStyle}>{holding.quantity?.toLocaleString() ?? '—'}</div>
      <div className="gc r num" style={rowStyle}>${livePrice.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
      <div className="gc r num" style={rowStyle}>${(holding.avgCost ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
      <div className="gc r num" style={rowStyle}>${liveValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
      <div className={`gc r num ${pos ? 'pos' : 'neg'}`} style={rowStyle}>{pos ? '+' : ''}{sessionPct.toFixed(2)}%</div>
      <div className="gc" style={{ ...rowStyle, display: 'flex', alignItems: 'center' }}>
        <ChevronIcon size={14} style={{ color: 'var(--ink-3)' }} />
      </div>
    </div>
  );
}
