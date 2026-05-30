import Sparkline from '../shared/Sparkline';

interface SummaryCardsProps {
  totalValue: number;
  priceHistory: number[];
  cash: number;
  currencyCode: string;
  returnPct: number;
  absoluteReturn: number;
}

function fmt(value: number, currency: string) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value);
}

export default function SummaryCards({
  totalValue,
  priceHistory,
  cash,
  currencyCode,
  returnPct,
  absoluteReturn,
}: SummaryCardsProps) {
  const returnPos = returnPct >= 0;

  return (
    <div className="kpis">
      <div className="kpi live">
        <div className="kpi-lbl">Portfolio value</div>
        <div className="kpi-val">{fmt(totalValue, currencyCode)}</div>
        <Sparkline data={priceHistory} color={returnPos ? 'var(--pos)' : 'var(--neg)'} />
      </div>

      <div className="kpi">
        <div className="kpi-lbl">Total return</div>
        <div className={`kpi-val ${returnPos ? 'pos' : 'neg'}`}>
          {returnPos ? '+' : ''}{returnPct.toFixed(2)}%
        </div>
        <div className={`kpi-sub ${returnPos ? 'pos' : 'neg'}`}>
          {returnPos ? '+' : ''}{fmt(absoluteReturn, currencyCode)}
        </div>
      </div>

      <div className="kpi">
        <div className="kpi-lbl">Cash balance</div>
        <div className="kpi-val">{fmt(cash, currencyCode)}</div>
      </div>
    </div>
  );
}
