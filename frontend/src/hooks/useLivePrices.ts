import { useState, useEffect, useRef } from 'react';
import { useMarketWebSocket } from './useMarketWebSocket';

interface UseLivePricesReturn {
  prices: Map<string, number>;
  dayChangePct: Map<string, number>;
  lastUpdatedAt: Date | null;
  connected: boolean;
}

interface Pending {
  prices: Map<string, number>;
  dayChangePct: Map<string, number>;
  lastAt: Date | null;
  dirty: boolean;
}

function emptyPending(): Pending {
  return { prices: new Map(), dayChangePct: new Map(), lastAt: null, dirty: false };
}

export function useLivePrices(token: string | null): UseLivePricesReturn {
  const { lastPrice, connected } = useMarketWebSocket(token);
  const [prices, setPrices]               = useState<Map<string, number>>(new Map());
  const [dayChangePct, setDayChangePct]   = useState<Map<string, number>>(new Map());
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const baselineRef = useRef<Map<string, number>>(new Map());
  const pendingRef  = useRef<Pending>(emptyPending());

  useEffect(() => {
    baselineRef.current = new Map();
    pendingRef.current  = emptyPending();
    setPrices(new Map());
    setDayChangePct(new Map());
    setLastUpdatedAt(null);
  }, [token]);

  useEffect(() => {
    if (!lastPrice?.ticker || lastPrice.price == null) return;
    const { ticker, price } = lastPrice;

    if (!baselineRef.current.has(ticker)) {
      baselineRef.current.set(ticker, price);
    }
    const baseline = baselineRef.current.get(ticker);
    const pct = baseline ? ((price - baseline) / baseline) * 100 : 0;

    pendingRef.current.prices.set(ticker, price);
    pendingRef.current.dayChangePct.set(ticker, pct);
    pendingRef.current.lastAt = new Date();
    pendingRef.current.dirty  = true;
  }, [lastPrice]);

  useEffect(() => {
    const id = setInterval(() => {
      if (!pendingRef.current.dirty) return;
      pendingRef.current.dirty = false;
      setPrices(new Map(pendingRef.current.prices));
      setDayChangePct(new Map(pendingRef.current.dayChangePct));
      if (pendingRef.current.lastAt) setLastUpdatedAt(pendingRef.current.lastAt);
    }, 500);
    return () => clearInterval(id);
  }, []);

  return { prices, dayChangePct, lastUpdatedAt, connected };
}
