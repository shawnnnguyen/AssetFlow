import DataTable from '../shared/DataTable';
import HoldingRow from './HoldingRow';

const COLS = [
  { label: 'Asset',    width: '1.6fr' },
  { label: 'Qty',      width: '0.8fr', align: 'right' },
  { label: 'Avg cost', width: '1fr',   align: 'right' },
];

export default function HoldingsTable({ holdings = [], livePrices, lastUpdated }) {
  const rows = holdings.map(h => ({ ...h, id: h.holdingId ?? h.id }));

  return (
    <DataTable
      title="Holdings"
      meta={`${holdings.length} position${holdings.length !== 1 ? 's' : ''}${lastUpdated ? ` · ${lastUpdated}` : ''}`}
      columns={COLS}
      rows={rows}
      emptyMessage="No holdings yet. Record a transaction to get started."
      renderRow={h => <HoldingRow holding={h} livePrices={livePrices} />}
    />
  );
}
