import { useState, useEffect, useRef } from 'react';
import { useMarketWebSocket } from './useMarketWebSocket';

export function useLivePrices(token) {
  const { lastPrice, connected } = useMarketWebSocket(token);
  const [prices, setPrices]         = useState(new Map());
  const [dayChangePct, setDayChangePct] = useState(new Map());
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const baselineRef = useRef(new Map());

  useEffect(() => {
    if (!lastPrice?.ticker || !lastPrice?.price) return;
    const { ticker, price } = lastPrice;

    if (!baselineRef.current.has(ticker)) {
      baselineRef.current.set(ticker, price);
    }
    const baseline = baselineRef.current.get(ticker);
    const pct = baseline ? ((price - baseline) / baseline) * 100 : 0;

    setPrices(prev => new Map(prev).set(ticker, price));
    setDayChangePct(prev => new Map(prev).set(ticker, pct));
    setLastUpdatedAt(new Date());
  }, [lastPrice]);

  return { prices, dayChangePct, lastUpdatedAt, connected };
}
