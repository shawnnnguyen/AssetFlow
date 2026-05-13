import { useState } from 'react';
import { XIcon } from '../shared/Icons';
import EditAlertModal from '../modals/EditAlertModal';

export default function AlertRow({ alert, onDelete, onUpdate }) {
  const [editing, setEditing] = useState(false);
  const numTarget  = Number(alert.targetPrice);
  const numCurrent = Number(alert.currentPrice);
  const dir = !Number.isFinite(numCurrent) || !Number.isFinite(numTarget)
    ? '—'
    : numTarget > numCurrent ? '▲ above' : '▼ below';

  return (
    <>
      <div className="alert-card" onClick={() => setEditing(true)} style={{ cursor: 'pointer' }}>
        <div className="alert-header">
          <span className="sym">{alert.ticker}</span>
          <button
            className="alert-x"
            onClick={e => { e.stopPropagation(); onDelete(alert.id); }}
            title="Remove alert"
          >
            <XIcon size={12} />
          </button>
        </div>
        <div className="alert-cond">
          {dir} {Number.isFinite(numTarget) ? `$${numTarget.toFixed(2)}` : '—'}
        </div>
        <div className="alert-meta">
          <span>current {Number.isFinite(numCurrent) ? `$${numCurrent.toFixed(2)}` : '—'}</span>
          <span>{alert.createdAt ? new Date(alert.createdAt).toLocaleDateString() : ''}</span>
        </div>
      </div>

      {editing && (
        <EditAlertModal
          alert={alert}
          onClose={() => setEditing(false)}
          onUpdated={updated => { onUpdate?.(updated); setEditing(false); }}
        />
      )}
    </>
  );
}
