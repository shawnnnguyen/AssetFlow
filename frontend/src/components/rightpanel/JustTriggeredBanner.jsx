export default function JustTriggeredBanner({ alerts = [] }) {
  if (alerts.length === 0) return null;
  return (
    <div className="rail-section">
      <div className="rail-lbl">
        <span style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--warn)', display: 'inline-block' }} />
        Just triggered
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {alerts.map((a, i) => (
          <div key={i} className="toast">
            <div className="toast-title">{a.ticker} alert fired</div>
            <div className="toast-msg">
              Target ${a.targetPrice?.toFixed(2)} · triggered at ${a.currentPrice?.toFixed(2)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
