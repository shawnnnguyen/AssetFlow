import { XIcon } from '../shared/Icons';

export default function AlertRow({ alert, onDelete }) {
  const dir = alert.targetPrice > (alert.currentPrice ?? 0) ? '▲ above' : '▼ below';

  return (
    <div className="alert-card">
      <div className="alert-header">
        <span className="sym">{alert.ticker}</span>
        <button className="alert-x" onClick={() => onDelete(alert.id)} title="Remove alert">
          <XIcon size={12} />
        </button>
      </div>
      <div className="alert-cond">
        {dir} ${alert.targetPrice?.toFixed(2)}
      </div>
      <div className="alert-meta">
        <span>current ${alert.currentPrice?.toFixed(2) ?? '—'}</span>
        <span>{alert.createdAt ? new Date(alert.createdAt).toLocaleDateString() : ''}</span>
      </div>
    </div>
  );
}
