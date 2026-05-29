import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import type { TrackedStock, TrackedStockMap, CompanyProfileCache, CachedProfile } from '../types';

export function useTrackedStocks(userId: string | null) {
  const [trackedStocks, setTrackedStocks]   = useState<TrackedStock[]>([]);
  const [trackedCount, setTrackedCount]     = useState(0);
  const [companyProfiles, setCompanyProfiles] = useState<CompanyProfileCache>({});

  const trackedStocksRef  = useRef<TrackedStockMap>({});
  const companyNameCache  = useRef<CompanyProfileCache>({});
  const fetchingTickers   = useRef<Set<string>>(new Set());

  useEffect(() => {
    if (!userId) return;
    trackedStocksRef.current = {};
    setTrackedStocks([]);
    setTrackedCount(0);
    setCompanyProfiles({});
    let stale = false;
    void api.market.getTrackedStocks()
      .then(tracked => {
        if (stale) return;
        const trackedMap: TrackedStockMap = {};
        tracked.forEach(s => { trackedMap[s.assetId] = s; });
        trackedStocksRef.current = trackedMap;
        setTrackedStocks(tracked);
        setTrackedCount(tracked.length);
        fetchCompanyProfiles(tracked.map(s => s.ticker));
      })
      .catch(console.error);
    return () => { stale = true; };
  }, [userId]);

  function fetchCompanyProfiles(tickers: string[]) {
    const unique = [...new Set(tickers.filter(t => t && t !== '—'))];
    const fromCache: CompanyProfileCache = {};
    const toFetch: string[] = [];

    unique.forEach(ticker => {
      const cached = companyNameCache.current[ticker];
      if (cached !== undefined) {
        fromCache[ticker] = cached;
      } else if (!fetchingTickers.current.has(ticker)) {
        toFetch.push(ticker);
      }
    });

    if (Object.keys(fromCache).length > 0) {
      setCompanyProfiles(prev => ({ ...prev, ...fromCache }));
    }

    toFetch.forEach(ticker => {
      fetchingTickers.current.add(ticker);
      void api.market.getProfile(ticker)
        .then(p => {
          if (!p) return;
          const profile: CachedProfile = { name: p.name ?? '', industry: p.industry ?? '' };
          companyNameCache.current[ticker] = profile;
          fetchingTickers.current.delete(ticker);
          setCompanyProfiles(prev => ({ ...prev, [ticker]: profile }));
        })
        .catch(() => { fetchingTickers.current.delete(ticker); });
    });
  }

  async function handleRemoveTracking(ticker: string) {
    try {
      await api.market.removeTracking(ticker);
      const next = trackedStocks.filter(s => s.ticker !== ticker);
      setTrackedStocks(next);
      setTrackedCount(next.length);
    } catch (e) {
      console.error('Failed to untrack', ticker, e);
    }
  }

  return {
    trackedStocks,
    setTrackedStocks,
    trackedCount,
    trackedStocksRef,
    companyProfiles,
    fetchCompanyProfiles,
    handleRemoveTracking,
  };
}
