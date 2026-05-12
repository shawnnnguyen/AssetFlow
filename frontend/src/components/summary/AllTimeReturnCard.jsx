export default function AllTimeReturnCard({ returnPct = 0, absoluteReturn = 0 }) {
  const pos = returnPct >= 0;
  return (
    <div className="kpi">
      <div className="kpi-lbl">All-time return</div>
      <div className="kpi-val">
        {pos ? '+' : ''}{returnPct.toFixed(1)}%
      </div>
      <div className={`kpi-sub ${pos ? 'pos' : 'neg'}`}>
        {pos ? '+' : ''}${Math.abs(absoluteReturn).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} vs cost basis
      </div>
    </div>
  );
}
