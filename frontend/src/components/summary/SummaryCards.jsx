import TotalValueCard from './TotalValueCard';
import CashCard from './CashCard';
import AllTimeReturnCard from './AllTimeReturnCard';
import PortfolioStatusCard from './PortfolioStatusCard';

export default function SummaryCards({ totalValue, sessionChange, priceHistory, cash, currencyCode, returnPct, absoluteReturn, portfolioStatus, portfolioCreatedAt }) {
  return (
    <div className="kpis">
      <TotalValueCard
        value={totalValue}
        sessionChange={sessionChange}
        priceHistory={priceHistory}
      />
      <CashCard cash={cash} currencyCode={currencyCode} />
      <AllTimeReturnCard returnPct={returnPct} absoluteReturn={absoluteReturn} />
      <PortfolioStatusCard status={portfolioStatus} createdAt={portfolioCreatedAt} />
    </div>
  );
}
