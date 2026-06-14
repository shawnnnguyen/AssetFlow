import { memo, useState, useEffect, useRef } from 'react';
import type { DisplayHolding } from '../../types';

interface HoldingRowProps {
  holding: DisplayHolding;
  livePrice: number;
}

function HoldingRow({ holding, livePrice }: HoldingRowProps) {
  const avgCost = holding.avgCost ?? 0;
  const pnlPct    = avgCost > 0 ? ((livePrice - avgCost) / avgCost) * 100 : 0;
  const pos       = pnlPct >= 0;

  const prevPriceRef = useRef<number>(livePrice);
  const [flash, setFlash] = useState<'' | 'flash-pos' | 'flash-neg'>('');

  useEffect(() => {
    if (livePrice === prevPriceRef.current) return;
    const cls = livePrice > prevPriceRef.current ? 'flash-pos' : 'flash-neg';
    prevPriceRef.current = livePrice;
    setFlash(cls);
    const t = setTimeout(() => setFlash(''), 600);
    return () => clearTimeout(t);
  }, [livePrice]);

  const cellBg: React.CSSProperties = flash
    ? { background: flash === 'flash-pos' ? 'var(--pos-soft)' : 'var(--neg-soft)', transition: 'background var(--dur) var(--ease)' }
    : { transition: 'background var(--dur) var(--ease)' };

  return (
    <div style={{ display: 'contents' }}>
      <div className="gc" style={cellBg}>
        <div className="sym">{holding.ticker ?? '—'}</div>
        {holding.companyName && <div className="holding-name">{holding.companyName}</div>}
      </div>
      <div className="gc r num" style={cellBg}>
        {holding.quantity.toLocaleString()}
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

export default memo(HoldingRow);
