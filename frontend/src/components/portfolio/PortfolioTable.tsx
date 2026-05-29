import DataTable from '../shared/DataTable';
import PortfolioRow from './PortfolioRow';
import type { Portfolio, PortfolioPerf, PortfolioRowData, TableColumn } from '../../types';

const COLS: TableColumn[] = [
  { label: 'Portfolio',   width: '1.8fr' },
  { label: 'Total value', width: '1fr',   align: 'right' },
  { label: 'Currency',    width: '0.6fr', align: 'right' },
];

interface PortfolioTableProps {
  portfolios?: Portfolio[];
  performanceMap?: Partial<Record<number, PortfolioPerf>>;
  onSelect?: (id: number) => void;
  onNew?: () => void;
}

export default function PortfolioTable({
  portfolios = [],
  performanceMap = {},
  onSelect,
  onNew,
}: PortfolioTableProps) {
  const rows: PortfolioRowData[] = portfolios.map(p => ({
    id: p.id,
    name: p.name,
    currencyCode: p.currencyCode,
    cashBalance: p.cashBalance,
    portfolioValue: performanceMap[p.id]?.portfolioValue,
  }));

  return (
    <DataTable
      title="Portfolios"
      meta={`${portfolios.length} portfolio${portfolios.length !== 1 ? 's' : ''}`}
      columns={COLS}
      rows={rows}
      emptyMessage="No portfolios yet. Create one to get started."
      headerActions={onNew && (
        <button className="btn" onClick={onNew} style={{ fontSize: 12 }}>+ New portfolio</button>
      )}
      renderRow={p => <PortfolioRow portfolio={p} onClick={onSelect} />}
    />
  );
}
