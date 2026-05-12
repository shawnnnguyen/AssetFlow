import { PlusIcon } from './shared/Icons';

export default function EmptyPortfolioState({ onCreatePortfolio }) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', height: '100%', gap: 12, color: 'var(--ink-3)'
    }}>
      <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--ink-2)' }}>
        Create your first portfolio
      </div>
      <div style={{ fontSize: 13, color: 'var(--ink-3)' }}>
        Track your investments across stocks, ETFs, and more.
      </div>
      <button className="btn primary" onClick={onCreatePortfolio} style={{ marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
        <PlusIcon size={14} /> New portfolio
      </button>
    </div>
  );
}
