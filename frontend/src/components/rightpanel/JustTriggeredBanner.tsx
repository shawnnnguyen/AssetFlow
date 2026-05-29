import type { TriggeredAlert } from '../../types';

interface JustTriggeredBannerProps {
  alerts: TriggeredAlert[];
}

export default function JustTriggeredBanner({ alerts }: JustTriggeredBannerProps) {
  if (alerts.length === 0) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {alerts.map((a, i) => {
        const id = a.priceAlertId ?? a.alertId ?? i;
        return (
          <div key={id} className="toast">
            <div className="toast-title">Alert triggered — {a.ticker ?? '—'}</div>
            <div className="toast-msg">
              target ${Number(a.targetPrice).toFixed(2)} · current ${Number(a.currentPrice).toFixed(2)}
            </div>
          </div>
        );
      })}
    </div>
  );
}
