import { useState, useEffect } from 'react';
import { api } from '../api';
import { useToast } from '../context/ToastContext';
import type { PriceAlert, NormalizedAlert, TrackedStockMap } from '../types';
import type { MutableRefObject } from 'react';

type AlertLike = {
  priceAlertId?: number;
  alertId?: number;
  ticker?: string;
  assetId?: number;
  targetPrice?: number;
  currentPrice?: number;
  createdAt?: string;
};

export function normalizeAlert(a: AlertLike, trackedMap: TrackedStockMap): NormalizedAlert {
  const id = a.priceAlertId ?? a.alertId ?? 0;
  const assetId = a.assetId ?? 0;
  const ticker = a.ticker ?? trackedMap[assetId]?.ticker ?? '—';
  const base: NormalizedAlert = {
    id,
    ticker,
    targetPrice:  Number(a.targetPrice ?? 0),
    currentPrice: Number(a.currentPrice ?? 0),
    assetId,
  };
  return a.createdAt !== undefined ? { ...base, createdAt: a.createdAt } : base;
}

export function useAlerts(
  userId: string | null,
  trackedStocksRef: MutableRefObject<TrackedStockMap>,
) {
  const { addToast } = useToast();
  const [alerts, setAlerts] = useState<NormalizedAlert[]>([]);

  useEffect(() => {
    if (!userId) return;
    setAlerts([]);
    let stale = false;
    void api.alerts.getAll()
      .then((rawAlerts: PriceAlert[]) => {
        if (stale) return;
        setAlerts(rawAlerts.map(a => normalizeAlert(a, trackedStocksRef.current)));
      })
      .catch(e => { console.error(e); addToast('Could not load alerts'); });
    return () => { stale = true; };
  }, [userId]);

  async function handleDeleteAlert(alertId: number) {
    try {
      await api.alerts.remove(alertId);
      setAlerts(prev => prev.filter(a => a.id !== alertId));
    } catch (e) {
      console.error(e);
      addToast('Could not delete alert');
    }
  }

  function handleUpdateAlert(updated: NormalizedAlert) {
    setAlerts(prev => prev.map(a => a.id === updated.id ? { ...a, ...updated } : a));
  }

  return { alerts, setAlerts, handleDeleteAlert, handleUpdateAlert };
}
