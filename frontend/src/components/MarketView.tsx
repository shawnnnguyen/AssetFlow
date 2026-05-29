import { useState, useEffect, useRef } from 'react';
import DataTable from './shared/DataTable';
import { XIcon } from './shared/Icons';
import type { TrackedStock, CompanyProfileCache, TableColumn } from '../types';

const COLS: TableColumn[] = [
  { label: 'Ticker',     width: '1.8fr' },
  { label: 'Industry',   width: '1.4fr' },
  { label: 'Live price', width: '1fr', align: 'right' },
  { label: '',           width: '2rem' },
];

type MarketRow = TrackedStock & { id: number };

interface MarketRowProps {
  s: MarketRow;
  livePrices: Map<string, number>;
  companyProfiles: CompanyProfileCache;
  onRemove?: ((ticker: string) => void) | undefined;
}

function MarketRowComponent({ s, livePrices, companyProfiles, onRemove }: MarketRowProps) {
  const livePrice = livePrices.get(s.ticker);
  const price: number | null = livePrice !== undefined ? livePrice : s.latestPrice;

  const prevPriceRef = useRef<number | null>(price);
  const [flash, setFlash] = useState<'' | 'flash-pos' | 'flash-neg'>('');

  useEffect(() => {
    if (price == null || price === prevPriceRef.current) return;
    const prev = prevPriceRef.current;
    const cls: 'flash-pos' | 'flash-neg' = prev !== null && price > prev ? 'flash-pos' : 'flash-neg';
    prevPriceRef.current = price;
    setFlash(cls);
    const t = setTimeout(() => setFlash(''), 600);
    return () => clearTimeout(t);
  }, [price]);

  const flashStyle: React.CSSProperties = flash
    ? { background: flash === 'flash-pos' ? 'var(--pos-soft)' : 'var(--neg-soft)', transition: 'background var(--dur) var(--ease)' }
    : { transition: 'background var(--dur) var(--ease)' };

  const profile = companyProfiles[s.ticker];

  return (
    <>
      <div className="gc" style={flashStyle}>
        <div className="sym">{s.ticker}</div>
        {profile?.name && <div className="holding-name">{profile.name}</div>}
      </div>
      <div className="gc" style={{ fontSize: '12.5px', color: 'var(--ink-3)', display: 'flex', alignItems: 'center', ...flashStyle }}>
        {profile?.industry ?? '—'}
      </div>
      <div className="gc r num" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', ...flashStyle }}>
        {price != null
          ? `$${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
          : '—'}
      </div>
      <div className="gc" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', ...flashStyle }}>
        <button className="alert-x" title="Untrack" onClick={() => onRemove?.(s.ticker)}>
          <XIcon size={12} />
        </button>
      </div>
    </>
  );
}

interface MarketViewProps {
  trackedStocks?: TrackedStock[];
  livePrices: Map<string, number>;
  lastUpdatedAt?: Date | null;
  companyProfiles?: CompanyProfileCache;
  onRemove?: ((ticker: string) => void) | undefined;
}

export default function MarketView({
  trackedStocks = [],
  livePrices,
  lastUpdatedAt,
  companyProfiles = {},
  onRemove,
}: MarketViewProps) {
  const rows: MarketRow[] = trackedStocks.map(s => ({ ...s, id: s.assetId }));

  return (
    <DataTable
      title="Market"
      meta={`${trackedStocks.length} tracked${lastUpdatedAt ? ` · ${lastUpdatedAt.toLocaleTimeString()}` : ' · connecting…'}`}
      columns={COLS}
      rows={rows}
      emptyMessage="No tracked tickers. Use Find Ticker to start tracking."
      renderRow={s => (
        <MarketRowComponent
          s={s}
          livePrices={livePrices}
          companyProfiles={companyProfiles}
          onRemove={onRemove}
        />
      )}
    />
  );
}
