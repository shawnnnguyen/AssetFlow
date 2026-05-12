import Sparkline from '../shared/Sparkline';

export default function TotalValueCard({ value = 0, sessionChange = 0, priceHistory = [] }) {
  const pos = sessionChange >= 0;
  const pct = value ? (Math.abs(sessionChange) / value * 100).toFixed(2) : '0.00';

  return (
    <div className="kpi live">
      <div className="kpi-lbl">
        <span className="live-dot" />
        Total value · live
      </div>
      <div className="kpi-val">
        ${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </div>
      <div className={`kpi-sub ${pos ? 'pos' : 'neg'}`}>
        {pos ? '+' : '−'}${Math.abs(sessionChange).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        {'  ·  '}{pos ? '+' : '−'}{pct}% session
      </div>
      <Sparkline data={priceHistory.length >= 2 ? priceHistory : [value, value]} />
    </div>
  );
}
