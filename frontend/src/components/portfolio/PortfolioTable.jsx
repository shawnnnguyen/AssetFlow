import PortfolioRow from './PortfolioRow';

export default function PortfolioTable({ portfolios = [], performanceMap = {}, onSelect, onNew }) {
  return (
    <div className="panel">
      <div className="phead">
        <div>
          <h2>Portfolios</h2>
          <div className="meta">{portfolios.length} portfolio{portfolios.length !== 1 ? 's' : ''}</div>
        </div>
        {onNew && (
          <button className="btn" onClick={onNew} style={{ fontSize: 12 }}>+ New portfolio</button>
        )}
      </div>

      <div className="portfolio-grid">
        <div className="gh">Name</div>
        <div className="gh r">Value</div>
        <div className="gh r">Cash</div>
        <div className="gh r">Return</div>
        <div className="gh r">Currency</div>
        <div className="gh" />
      </div>

      {portfolios.map(p => (
        <PortfolioRow
          key={p.id}
          portfolio={{ ...p, ...performanceMap[p.id] }}
          onClick={onSelect}
        />
      ))}
    </div>
  );
}
