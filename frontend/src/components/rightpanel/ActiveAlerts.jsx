import AlertRow from './AlertRow';

export default function ActiveAlerts({ alerts = [], onDelete, onUpdate }) {
  return (
    <div className="rail-section">
      <div className="rail-lbl">Active alerts ({alerts.length})</div>
      {alerts.length === 0 ? (
        <div style={{ fontSize: '12.5px', color: 'var(--ink-3)' }}>No active alerts.</div>
      ) : (
        <div className="alert-list">
          {alerts.map(a => (
            <AlertRow key={a.id} alert={a} onDelete={onDelete} onUpdate={onUpdate} />
          ))}
        </div>
      )}
    </div>
  );
}
