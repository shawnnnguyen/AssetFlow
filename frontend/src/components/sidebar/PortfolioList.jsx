import { PlusIcon } from '../shared/Icons';

export default function PortfolioList({ portfolios = [], currentId, onSelect, onNew }) {
  return (
    <div className="pf-list">
      {portfolios.map(p => (
        <div
          key={p.id}
          className={`pf${currentId === p.id ? ' on' : ''}`}
          onClick={() => onSelect(p.id)}
        >
          <span className="pf-name">{p.name}</span>
          <span className="pf-ccy">{p.currency || 'USD'}</span>
        </div>
      ))}
      <div className="pf pf-add" onClick={onNew}>
        <PlusIcon size={13} />
        <span className="pf-name">New portfolio</span>
      </div>
    </div>
  );
}
