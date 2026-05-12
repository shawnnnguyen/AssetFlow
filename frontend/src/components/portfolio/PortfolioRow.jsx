import { ChevronIcon } from '../shared/Icons';

export default function PortfolioRow({ portfolio, onClick }) {
  const { id, name, currencyCode, cashBalance, portfolioValue = 0, returnPct = 0 } = portfolio;
  const pos = returnPct >= 0;

  return (
    <div className="portfolio-grid" style={{ display: 'contents', cursor: 'pointer' }} onClick={() => onClick(id)}>
      <div className="gc">
        <div className="sym">{name}</div>
        <div className="holding-name">{currencyCode ?? 'USD'}</div>
      </div>
      <div className="gc r num">
        ${(portfolioValue ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </div>
      <div className="gc r num">
        ${(cashBalance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </div>
      <div className={`gc r num ${pos ? 'pos' : 'neg'}`}>
        {pos ? '+' : ''}{returnPct.toFixed(2)}%
      </div>
      <div className="gc r" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{currencyCode ?? 'USD'}</div>
      <div className="gc" style={{ display: 'flex', alignItems: 'center' }}>
        <ChevronIcon size={14} style={{ color: 'var(--ink-3)' }} />
      </div>
    </div>
  );
}
