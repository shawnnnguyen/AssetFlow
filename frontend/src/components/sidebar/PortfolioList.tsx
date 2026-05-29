import { PlusIcon } from '../shared/Icons';
import type { Portfolio } from '../../types';

interface PortfolioListProps {
  portfolios?: Portfolio[];
  currentId: number | null;
  onSelect: (id: number) => void;
  onNew: () => void;
}

export default function PortfolioList({ portfolios = [], currentId, onSelect, onNew }: PortfolioListProps) {
  return (
    <div className="pf-list">
      {portfolios.map(p => (
        <div
          key={p.id}
          className={`pf${currentId === p.id ? ' on' : ''}`}
          onClick={() => onSelect(p.id)}
        >
          <span className="pf-name">{p.name}</span>
          <span className="pf-ccy">{p.currencyCode}</span>
        </div>
      ))}
      <div className="pf pf-add" onClick={onNew}>
        <PlusIcon size={13} />
        <span className="pf-name">New portfolio</span>
      </div>
    </div>
  );
}
