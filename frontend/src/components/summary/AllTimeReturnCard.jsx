export default function AllTimeReturnCard({ returnPct = 0, absoluteReturn = 0, currencyCode = 'USD' }) {
  const pos = returnPct >= 0;
  let formatted;
  try {
    formatted = new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Math.abs(absoluteReturn));
  } catch {
    formatted = `${currencyCode} ${Math.abs(absoluteReturn).toFixed(2)}`;
  }
  return (
    <div className="kpi">
      <div className="kpi-lbl">All-time return</div>
      <div className="kpi-val">
        {pos ? '+' : ''}{returnPct.toFixed(1)}%
      </div>
      <div className={`kpi-sub ${pos ? 'pos' : 'neg'}`}>
        {pos ? '+' : ''}{formatted} vs cost basis
      </div>
    </div>
  );
}
