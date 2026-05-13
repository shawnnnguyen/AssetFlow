import Sparkline from '../shared/Sparkline';

export default function TotalValueCard({ value = 0, sessionChange = 0, priceHistory = [], currencyCode = 'USD' }) {
  const pos = sessionChange >= 0;
  const pct = value ? (Math.abs(sessionChange) / value * 100).toFixed(2) : '0.00';
  const fmt = (n) => {
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    } catch {
      return `${currencyCode} ${n.toFixed(2)}`;
    }
  };

  return (
    <div className="kpi live">
      <div className="kpi-lbl">
        <span className="live-dot" />
        Total value · live
      </div>
      <div className="kpi-val">
        {fmt(value)}
      </div>
      <div className={`kpi-sub ${pos ? 'pos' : 'neg'}`}>
        {pos ? '+' : '−'}{fmt(Math.abs(sessionChange))}
        {'  ·  '}{pos ? '+' : '−'}{pct}% session
      </div>
      <Sparkline data={priceHistory.length >= 2 ? priceHistory : [value, value]} />
    </div>
  );
}
