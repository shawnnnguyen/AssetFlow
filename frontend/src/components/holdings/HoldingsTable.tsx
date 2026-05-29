import DataTable from '../shared/DataTable';
import HoldingRow from './HoldingRow';
import type { DisplayHolding, TableColumn } from '../../types';

const COLS: TableColumn[] = [
  { label: 'Asset',    width: '1.6fr' },
  { label: 'Qty',      width: '0.8fr', align: 'right' },
  { label: 'Avg cost', width: '1fr',   align: 'right' },
];

interface HoldingsTableProps {
  holdings?: DisplayHolding[];
  livePrices: Map<string, number>;
  lastUpdated?: Date | string | null;
}

export default function HoldingsTable({ holdings = [], livePrices, lastUpdated }: HoldingsTableProps) {
  const rows = holdings.map(h => ({ ...h, id: h.holdingId }));

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
