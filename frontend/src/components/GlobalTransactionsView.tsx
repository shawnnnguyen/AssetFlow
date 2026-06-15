import { useState, useEffect, useMemo } from 'react';
import type { MutableRefObject } from 'react';
import { api } from '../api';
import type { Portfolio, Transaction, TrackedStockMap, CompanyProfileCache, TableColumn } from '../types';
import DataTable from './shared/DataTable';

const COLS: TableColumn[] = [
  { label: 'Date',      width: '1.2fr' },
  { label: 'Portfolio', width: '1.2fr' },
  { label: 'Type',      width: '0.6fr' },
  { label: 'Ticker',    width: '0.8fr' },
  { label: 'Company',   width: '1.6fr' },
  { label: 'Qty',       width: '0.8fr', align: 'right' },
  { label: 'Price',     width: '1fr',   align: 'right' },
];

interface RowData {
  id: number;
  date: string;
  portfolioName: string;
  type: 'BUY' | 'SELL';
  ticker: string;
  companyName: string;
  quantity: number;
  price: number;
}

interface GlobalTransactionsViewProps {
  trackedStocksRef: MutableRefObject<TrackedStockMap>;
  portfolios: Portfolio[];
  companyProfiles: CompanyProfileCache;
  refreshKey: number;
}

export default function GlobalTransactionsView({
  trackedStocksRef,
  portfolios,
  companyProfiles,
  refreshKey,
}: GlobalTransactionsViewProps) {
  const [rawTx, setRawTx] = useState<Transaction[]>([]);

  useEffect(() => {
    let stale = false;
    void api.transactions.getAll(100).then(page => {
      if (stale) return;
      setRawTx(page.content ?? []);
    }).catch(console.error);
    return () => { stale = true; };
  }, [refreshKey, portfolios]);

  const rows = useMemo<RowData[]>(() => {
    const pfMap: Record<number, string> = {};
    for (const p of portfolios) pfMap[p.id] = p.name;
    const trackedMap = trackedStocksRef.current;

    return rawTx.map(tx => {
      const ticker = trackedMap[tx.assetId]?.ticker ?? '—';
      return {
        id:            tx.transactionId,
        date:          new Date(tx.executedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
        portfolioName: pfMap[tx.portfolioId] ?? `#${tx.portfolioId}`,
        type:          tx.type,
        ticker,
        companyName:   companyProfiles[ticker]?.name ?? '',
        quantity:      tx.quantity,
        price:         tx.pricePerUnit,
      };
    });
  }, [rawTx, portfolios, companyProfiles]);

  return (
    <DataTable
      title="All transactions"
      meta={`${rows.length} transaction${rows.length !== 1 ? 's' : ''}`}
      columns={COLS}
      rows={rows}
      emptyMessage="No transactions yet."
      renderRow={row => (
        <>
          <div className="gc">{row.date}</div>
          <div className="gc">{row.portfolioName}</div>
          <div className="gc">
            <span className={`tx-key ${row.type === 'BUY' ? 'b' : 's'}`}>{row.type}</span>
          </div>
          <div className="gc">{row.ticker}</div>
          <div className="gc" style={{ color: 'var(--ink-2)' }}>{row.companyName}</div>
          <div className="gc num r">{row.quantity.toLocaleString('en-US', { maximumFractionDigits: 4 })}</div>
          <div className="gc num r">${row.price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
        </>
      )}
    />
  );
}
