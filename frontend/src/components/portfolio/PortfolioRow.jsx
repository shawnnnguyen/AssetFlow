export default function PortfolioRow({ portfolio, onClick }) {
  const { id, name, currencyCode, currency, cashBalance, portfolioValue } = portfolio;
  const displayCurrency = currencyCode ?? currency ?? 'USD';
  const totalValue = portfolioValue || cashBalance || 0;
  let formattedValue;
  try {
    formattedValue = new Intl.NumberFormat(undefined, { style: 'currency', currency: displayCurrency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(totalValue);
  } catch {
    formattedValue = `${displayCurrency} ${totalValue.toFixed(2)}`;
  }

  return (
    <div className="dt-row-click" onClick={() => onClick?.(id)}>
      <div className="gc">
        <span style={{ fontWeight: 500, fontSize: '13.5px' }}>{name}</span>
      </div>
      <div className="gc r num">
        {formattedValue}
      </div>
      <div className="gc r" style={{ fontSize: '12px', color: 'var(--ink-3)' }}>
        {displayCurrency}
      </div>
    </div>
  );
}
