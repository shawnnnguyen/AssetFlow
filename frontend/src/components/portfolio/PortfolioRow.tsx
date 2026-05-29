import type { PortfolioRowData } from '../../types';

interface PortfolioRowProps {
  portfolio: PortfolioRowData;
  onClick?: ((id: number) => void) | undefined;
}

export default function PortfolioRow({ portfolio, onClick }: PortfolioRowProps) {
  const { id, name, currencyCode, cashBalance, portfolioValue } = portfolio;
  const totalValue = portfolioValue ?? cashBalance ?? 0;
  let formattedValue: string;
  try {
    formattedValue = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(totalValue);
  } catch {
    formattedValue = `${currencyCode} ${totalValue.toFixed(2)}`;
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
        {currencyCode}
      </div>
    </div>
  );
}
