export default function CashCard({ cash = 0, currencyCode = 'USD' }) {
  const code = currencyCode || 'USD';
  const formatted = new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: code,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(cash);

  return (
    <div className="kpi">
      <div className="kpi-lbl">Cash</div>
      <div className="kpi-val">{formatted}</div>
      <div className="kpi-sub">{code}</div>
    </div>
  );
}
