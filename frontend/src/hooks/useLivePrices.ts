import { useState, useEffect, useRef } from 'react';
import { useMarketWebSocket } from './useMarketWebSocket';

interface UseLivePricesReturn {
  prices: Map<string, number>;
  dayChangePct: Map<string, number>;
  lastUpdatedAt: Date | null;
  connected: boolean;
}

export function useLivePrices(token: string | null): UseLivePricesReturn {
  const { lastPrice, connected } = useMarketWebSocket(token);
  const [prices, setPrices]             = useState<Map<string, number>>(new Map());
  const [dayChangePct, setDayChangePct] = useState<Map<string, number>>(new Map());
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const baselineRef = useRef<Map<string, number>>(new Map());

  useEffect(() => {
    baselineRef.current = new Map();
    setPrices(new Map());
    setDayChangePct(new Map());
  }, [token]);

  useEffect(() => {
    if (!lastPrice?.ticker || lastPrice.price == null) return;
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
