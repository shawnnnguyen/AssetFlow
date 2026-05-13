import { SearchIcon, PlusIcon, BellIcon, ArrowUpDownIcon } from '../shared/Icons';

export default function DashboardHeader({ title, onBack, onFindTicker, onNewAlert, onRecordTransaction, onDeposit }) {
  return (
    <div className="top">
      <div>
        {onBack && (
          <button className="back-link" onClick={onBack}>← Portfolios</button>
        )}
        <div className="top-title">{title || 'Portfolios'}</div>
      </div>
      <div className="top-actions">
        {onFindTicker && (
          <button className="btn" onClick={onFindTicker}>
            <SearchIcon size={14} /> Find ticker
          </button>
        )}
        {onNewAlert && (
          <button className="btn" onClick={onNewAlert}>
            <BellIcon size={14} /> New price alert
          </button>
        )}
        {onDeposit && (
          <button className="btn" onClick={onDeposit}>
            <ArrowUpDownIcon size={14} /> Deposit / Withdraw
          </button>
        )}
        {onRecordTransaction && (
          <button className="btn primary" onClick={onRecordTransaction}>
            <PlusIcon size={14} /> Record transaction
          </button>
        )}
      </div>
    </div>
  );
}
