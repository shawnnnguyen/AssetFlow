import { useState, useEffect } from 'react';
import type { MutableRefObject } from 'react';
import { api } from '../api';
import { useToast } from '../context/ToastContext';
import type { Transaction, DisplayTransaction, TrackedStockMap } from '../types';

function normalizeTx(tx: Transaction, trackedMap: TrackedStockMap): DisplayTransaction {
  return {
    id:              tx.transactionId,
    transactionType: tx.type,
    ticker:          trackedMap[tx.assetId]?.ticker ?? '—',
    quantity:        tx.quantity,
    price:           tx.pricePerUnit,
  };
}

export function useTransactions(
  portfolioId: number | null,
  trackedStocksRef: MutableRefObject<TrackedStockMap>,
) {
  const { addToast } = useToast();
  const [transactions, setTransactions] = useState<DisplayTransaction[]>([]);
  const [txRefreshKey, setTxRefreshKey] = useState(0);

  useEffect(() => {
    if (!portfolioId) {
      setTransactions([]);
      return;
    }
    let stale = false;
    void api.transactions.getPortfolio(portfolioId, 10)
      .then(res => {
        if (stale) return;
        const trackedMap = trackedStocksRef.current;
        setTransactions((res.content ?? []).map(tx => normalizeTx(tx, trackedMap)));
      })
      .catch(e => { console.error(e); addToast('Could not load transactions'); });
    return () => { stale = true; };
  }, [portfolioId, txRefreshKey]);

  function bumpRefreshKey() {
    setTxRefreshKey(k => k + 1);
  }

  return { transactions, txRefreshKey, bumpRefreshKey };
}
