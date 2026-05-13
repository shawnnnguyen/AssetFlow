import DataTable from '../shared/DataTable';
import PortfolioRow from './PortfolioRow';

const COLS = [
  { label: 'Portfolio',   width: '1.8fr' },
  { label: 'Total value', width: '1fr',   align: 'right' },
  { label: 'Currency',    width: '0.6fr', align: 'right' },
];

export default function PortfolioTable({ portfolios = [], performanceMap = {}, onSelect, onNew }) {
  const rows = portfolios.map(p => ({ id: p.id, ...p, ...performanceMap[p.id] }));

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
