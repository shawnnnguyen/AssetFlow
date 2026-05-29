import { useState, useEffect, useRef } from 'react';

export function useSparkline(
  livePortfolioValue: number,
  holdingsCount: number,
  initialPortfolioValue: number | null,
  resetKey: number | null, // currentPortfolioId — changes signal a portfolio switch
) {
  const [sparklineData, setSparklineData] = useState<number[]>([]);
  const priceHistoryRef    = useRef<number[]>([]);
  const lastPushRef        = useRef<number>(0);
  const sessionBaselineRef = useRef<number | null>(null);
  const seededRef          = useRef(false);

  // Reset all sparkline state when the portfolio changes.
  useEffect(() => {
    priceHistoryRef.current    = [];
    lastPushRef.current        = 0;
    sessionBaselineRef.current = null;
    seededRef.current          = false;
    setSparklineData([]);
  }, [resetKey]);

  // Seed from API-derived value once after load, so the chart isn't empty
  // while waiting for the first WebSocket price tick.
  useEffect(() => {
    if (seededRef.current || initialPortfolioValue === null || initialPortfolioValue <= 0) return;
    seededRef.current          = true;
    priceHistoryRef.current    = [initialPortfolioValue, initialPortfolioValue];
    lastPushRef.current        = Date.now();
    sessionBaselineRef.current = initialPortfolioValue;
    setSparklineData([initialPortfolioValue, initialPortfolioValue]);
  }, [initialPortfolioValue]);

  // Throttled live update: minimum 2 s between pushes, cap at 60 entries.
  useEffect(() => {
    if (holdingsCount === 0) return;
    const now = Date.now();
    if (now - lastPushRef.current < 2000) return;
    lastPushRef.current = now;
    if (sessionBaselineRef.current === null) {
      sessionBaselineRef.current = livePortfolioValue;
    }
    const updated = [...priceHistoryRef.current.slice(-59), livePortfolioValue];
    priceHistoryRef.current = updated;
    setSparklineData([...updated]);
  }, [livePortfolioValue, holdingsCount]);

  return { sparklineData };
}
