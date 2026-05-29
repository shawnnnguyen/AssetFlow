import { useState, useEffect } from 'react';
import type { MutableRefObject } from 'react';
import { api } from '../api';
import { usePortfolioWebSocket } from './usePortfolioWebSocket';
import type {
  Holding,
  HoldingPerf,
  PortfolioPerf,
  DisplayHolding,
  TrackedStockMap,
} from '../types';

function buildEnrichedHoldings(
  rawHoldings: Holding[],
  perf: PortfolioPerf,
  trackedMap: TrackedStockMap,
): DisplayHolding[] {
  const perfMap: Partial<Record<number, HoldingPerf>> = {};
  (perf.holdings ?? []).forEach(h => { perfMap[h.holdingId] = h; });

  return rawHoldings.map(h => ({
    holdingId:          h.holdingId,
    assetId:            h.assetId,
    quantity:           Number(h.quantity),
    avgCost:            Number(h.avgCost),
    ticker:             trackedMap[h.assetId]?.ticker ?? '—',
    currentMarketPrice: Number(perfMap[h.holdingId]?.currentMarketPrice ?? 0),
    absoluteChange:     Number(perfMap[h.holdingId]?.absoluteChange ?? 0),
    percentageChange:   Number(perfMap[h.holdingId]?.percentageChange ?? 0),
    companyName:        '', // enriched by DashboardPage's holdingsWithNames memo
  }));
}

export function useHoldings(
  portfolioId: number | null,
  token: string | null,
  currentPortfolioIdRef: MutableRefObject<number | null>,
  trackedStocksRef: MutableRefObject<TrackedStockMap>,
  fetchCompanyProfiles: (tickers: string[]) => void,
  onCashBalanceUpdate: (portfolioId: number, cashBalance: number) => void,
) {
  const [enrichedHoldings, setEnrichedHoldings] = useState<DisplayHolding[]>([]);
  const [performance, setPerformance]           = useState<PortfolioPerf | null>(null);
  const [performanceMap, setPerformanceMap]     = useState<Partial<Record<number, PortfolioPerf>>>({});

  const { portfolioPerf } = usePortfolioWebSocket(portfolioId, token);

  // Load on portfolio switch
  useEffect(() => {
    if (!portfolioId) {
      setEnrichedHoldings([]);
      setPerformance(null);
      return;
    }
    setEnrichedHoldings([]);
    setPerformance(null);
    let stale = false;
    void Promise.all([
      api.portfolios.getPerformance(portfolioId),
      api.holdings.getAll(portfolioId),
    ]).then(([perf, rawHoldings]) => {
      if (stale) return;
      const trackedMap = trackedStocksRef.current;
      setPerformance(perf);
      setPerformanceMap(prev => ({ ...prev, [portfolioId]: perf }));
      const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
      setEnrichedHoldings(enriched);
      fetchCompanyProfiles(enriched.map(h => h.ticker));
    }).catch(console.error);
    return () => { stale = true; };
  }, [portfolioId]);

  // WebSocket real-time performance updates
  useEffect(() => {
    if (!portfolioPerf || !portfolioId) return;
    setPerformance(portfolioPerf);
    onCashBalanceUpdate(portfolioId, portfolioPerf.cashBalance);

    const perfMap: Partial<Record<number, HoldingPerf>> = {};
    (portfolioPerf.holdings ?? []).forEach(h => { perfMap[h.holdingId] = h; });

    setEnrichedHoldings(prev => prev.map(h => {
      const p = perfMap[h.holdingId];
      if (!p) return h;
      return {
        ...h,
        currentMarketPrice: Number(p.currentMarketPrice),
        absoluteChange:     Number(p.absoluteChange),
        percentageChange:   Number(p.percentageChange),
      };
    }));
  }, [portfolioPerf]);

  // Re-fetch after recording a transaction; guards against portfolio switch mid-flight.
  async function refetch() {
    if (!portfolioId) return;
    const capturedId = portfolioId;
    const [perf, rawHoldings] = await Promise.all([
      api.portfolios.getPerformance(capturedId),
      api.holdings.getAll(capturedId),
    ]);
    if (currentPortfolioIdRef.current !== capturedId) return;
    const trackedMap = trackedStocksRef.current;
    setPerformance(perf);
    setPerformanceMap(prev => ({ ...prev, [capturedId]: perf }));
    const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
    setEnrichedHoldings(enriched);
    fetchCompanyProfiles(enriched.map(h => h.ticker));
  }

  return { enrichedHoldings, performance, performanceMap, refetch };
}
