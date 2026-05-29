import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api';
import { useLivePrices } from '../hooks/useLivePrices';
import { useAlertWebSocket } from '../hooks/useAlertWebSocket';
import { usePortfolios } from '../hooks/usePortfolios';
import { useTrackedStocks } from '../hooks/useTrackedStocks';
import { useAlerts, normalizeAlert } from '../hooks/useAlerts';
import { useHoldings } from '../hooks/useHoldings';
import { useTransactions } from '../hooks/useTransactions';
import { useDashboardModals } from '../hooks/useDashboardModals';
import { useSparkline } from '../hooks/useSparkline';
import Sidebar from '../components/sidebar/Sidebar';
import type { NavId } from '../components/sidebar/NavItem';
import DashboardHeader from '../components/header/DashboardHeader';
import SummaryCards from '../components/summary/SummaryCards';
import HoldingsTable from '../components/holdings/HoldingsTable';
import MarketView from '../components/MarketView';
import PortfolioTable from '../components/portfolio/PortfolioTable';
import GlobalTransactionsView from '../components/GlobalTransactionsView';
import RightPanel from '../components/rightpanel/RightPanel';
import RecordTransactionModal from '../components/modals/RecordTransactionModal';
import CashTransactionModal from '../components/modals/CashTransactionModal';
import FindTickerModal from '../components/modals/FindTickerModal';
import NewPortfolioModal from '../components/modals/NewPortfolioModal';
import NewPriceAlertModal from '../components/modals/NewPriceAlertModal';
import type { TriggeredAlert } from '../types';

export default function DashboardPage() {
  const { userId, token } = useAuth();

  const [activeNav, setActiveNav]         = useState<NavId>('dashboard');
  const [dashboardView, setDashboardView] = useState<'list' | 'detail'>('list');

  // ── Domain hooks ──────────────────────────────────────────────────────────
  const portfoliosHook = usePortfolios(userId);
  const trackedHook    = useTrackedStocks(userId);
  const alertsHook     = useAlerts(userId, trackedHook.trackedStocksRef);
  const holdingsHook   = useHoldings(
    portfoliosHook.currentPortfolioId,
    token,
    portfoliosHook.currentPortfolioIdRef,
    trackedHook.trackedStocksRef,
    trackedHook.fetchCompanyProfiles,
    (pfId, cashBalance) => {
      portfoliosHook.setPortfolios(prev =>
        prev.map(p => p.id === pfId ? { ...p, cashBalance } : p),
      );
    },
  );
  const txHook   = useTransactions(portfoliosHook.currentPortfolioId, trackedHook.trackedStocksRef);
  const modals   = useDashboardModals();

  const { prices: livePrices, lastUpdatedAt } = useLivePrices(token);
  const { triggeredAlerts } = useAlertWebSocket(userId, token);

  // ── Live portfolio value (for sparkline + summary) ─────────────────────
  const livePortfolioValue = useMemo(() =>
    holdingsHook.enrichedHoldings.reduce((sum, h) => {
      const price = livePrices.get(h.ticker) ?? h.currentMarketPrice ?? 0;
      return sum + price * h.quantity;
    }, 0),
  [holdingsHook.enrichedHoldings, livePrices]);

  const sparkline = useSparkline(
    livePortfolioValue,
    holdingsHook.enrichedHoldings.length,
    holdingsHook.performance?.portfolioValue ?? null,
    portfoliosHook.currentPortfolioId,
  );

  // ── Auto-splice triggered alerts from active list ──────────────────────
  useEffect(() => {
    if (!triggeredAlerts.length) return;
    const firedIds = new Set(
      triggeredAlerts
        .map(a => a.priceAlertId ?? a.alertId)
        .filter((id): id is number => id !== undefined),
    );
    // Fallback: if a triggered alert carried neither ID field, match by ticker.
    const firedTickers = new Set(
      triggeredAlerts
        .filter(a => a.priceAlertId === undefined && a.alertId === undefined && a.ticker !== undefined)
        .map(a => a.ticker as string),
    );
    alertsHook.setAlerts(prev =>
      prev.filter(a => !firedIds.has(a.id) && !firedTickers.has(a.ticker))
    );
  }, [triggeredAlerts]);

  // ── Reset to list view on nav change ───────────────────────────────────
  useEffect(() => {
    if (activeNav !== 'dashboard') setDashboardView('list');
  }, [activeNav]);

  // ── Normalised triggered alerts for the banner (resolved ticker) ───────
  const normalizedTriggeredAlerts = useMemo((): TriggeredAlert[] =>
    triggeredAlerts.map(a => ({
      ...a,
      ticker:       a.ticker ?? trackedHook.trackedStocksRef.current[a.assetId ?? -1]?.ticker ?? '—',
      targetPrice:  Number(a.targetPrice ?? 0),
      currentPrice: Number(a.currentPrice ?? 0),
    })),
  [triggeredAlerts]);

  // ── Holdings with company names resolved from profile cache ────────────
  const holdingsWithNames = useMemo(() =>
    holdingsHook.enrichedHoldings.map(h => ({
      ...h,
      companyName: trackedHook.companyProfiles[h.ticker]?.name ?? '',
    })),
  [holdingsHook.enrichedHoldings, trackedHook.companyProfiles]);

  // ── Derived summary values ──────────────────────────────────────────────
  const { currentPortfolio, currentPortfolioId } = portfoliosHook;
  const currencyCode   = currentPortfolio?.currencyCode ?? 'USD';
  const cash           = Number(currentPortfolio?.cashBalance ?? 0);
  // Use live-computed value once performance data has loaded (even if it's 0).
  // Before load, show 0 so the card isn't stale from a previous portfolio.
  const totalValue     = holdingsHook.performance !== null ? livePortfolioValue : 0;
  const totalInvested  = Number(holdingsHook.performance?.totalInvestedValue ?? 0);
  const absoluteReturn = totalInvested ? totalValue - totalInvested : 0;
  const returnPct      = totalInvested ? (absoluteReturn / totalInvested) * 100 : 0;

  // ── Handlers ───────────────────────────────────────────────────────────
  async function handleSelectPortfolio(id: number) {
    setDashboardView('detail');
    await portfoliosHook.handleSelectPortfolio(id);
  }

  return (
    <div className="dashboard-layout">
      <Sidebar
        activeNav={activeNav}
        setActiveNav={setActiveNav}
        portfolios={portfoliosHook.portfolios}
        currentPortfolioId={currentPortfolioId}
        setCurrentPortfolioId={handleSelectPortfolio}
        onNewPortfolio={modals.openPfModal}
      />

      <main className="main">
        <DashboardHeader
          title={
            activeNav === 'market'       ? 'Market'       :
            activeNav === 'transactions' ? 'Transactions' :
            dashboardView === 'detail'   ? currentPortfolio?.name : undefined
          }
          onBack={activeNav === 'dashboard' && dashboardView === 'detail'
            ? () => setDashboardView('list') : undefined}
          onFindTicker={dashboardView === 'detail' || activeNav === 'market'
            ? modals.openTickerModal : undefined}
          onNewAlert={dashboardView === 'detail' || activeNav === 'market'
            ? modals.openAlertModal : undefined}
          onDeposit={dashboardView === 'detail' ? modals.openCashModal : undefined}
          onRecordTransaction={dashboardView === 'detail' ? () => {
            if (!currentPortfolioId) { modals.openPfModal(); return; }
            modals.openTxModal();
          } : undefined}
        />

        {activeNav === 'dashboard' && dashboardView === 'list' && (
          <PortfolioTable
            portfolios={portfoliosHook.portfolios}
            performanceMap={holdingsHook.performanceMap}
            onSelect={handleSelectPortfolio}
            onNew={modals.openPfModal}
          />
        )}

        {activeNav === 'dashboard' && dashboardView === 'detail' && (
          <>
            <SummaryCards
              totalValue={totalValue}
              sessionChange={sparkline.sessionChange}
              priceHistory={sparkline.sparklineData}
              cash={cash}
              currencyCode={currencyCode}
              returnPct={returnPct}
              absoluteReturn={absoluteReturn}
            />
            <HoldingsTable
              holdings={holdingsWithNames}
              livePrices={livePrices}
              lastUpdated={lastUpdatedAt ? lastUpdatedAt.toLocaleTimeString() : null}
            />
          </>
        )}

        {activeNav === 'market' && (
          <MarketView
            trackedStocks={trackedHook.trackedStocks}
            livePrices={livePrices}
            lastUpdatedAt={lastUpdatedAt}
            companyProfiles={trackedHook.companyProfiles}
            onRemove={trackedHook.handleRemoveTracking}
          />
        )}

        {activeNav === 'transactions' && (
          <GlobalTransactionsView
            trackedStocksRef={trackedHook.trackedStocksRef}
            portfolios={portfoliosHook.portfolios}
            companyProfiles={trackedHook.companyProfiles}
            refreshKey={txHook.txRefreshKey}
          />
        )}
      </main>

      <RightPanel
        triggeredAlerts={normalizedTriggeredAlerts}
        activeAlerts={alertsHook.alerts}
        transactions={txHook.transactions}
        onDeleteAlert={alertsHook.handleDeleteAlert}
        onUpdateAlert={alertsHook.handleUpdateAlert}
      />

      {modals.showCashModal && currentPortfolioId !== null && (
        <CashTransactionModal
          portfolioId={currentPortfolioId}
          onSuccess={() => {
            txHook.bumpRefreshKey();
            holdingsHook.refetch().catch(console.error);
          }}
          onClose={modals.closeCashModal}
        />
      )}

      {modals.showTxModal && currentPortfolioId !== null && (
        <RecordTransactionModal
          onClose={modals.closeTxModal}
          onSuccess={async ({ transactionType, ticker, quantity }) => {
            const entry = Object.values(trackedHook.trackedStocksRef.current)
              .find(s => s.ticker === ticker);
            if (!entry) throw new Error(`"${ticker}" is not tracked. Use Find Ticker to add it first.`);
            await api.transactions.record(currentPortfolioId, {
              assetId:     entry.assetId,
              portfolioId: currentPortfolioId,
              executedAt:  new Date().toISOString(),
              quantity,
              type:        transactionType,
            });
            txHook.bumpRefreshKey();
            holdingsHook.refetch().catch(console.error);
          }}
        />
      )}

      {modals.showTickerModal && (
        <FindTickerModal onClose={modals.closeTickerModal} />
      )}

      {modals.showPfModal && (
        <NewPortfolioModal
          onClose={modals.closePfModal}
          onCreate={async body => {
            const newPf = await api.portfolios.create(body);
            const pfs   = await api.portfolios.getAll();
            portfoliosHook.setPortfolios(pfs);
            portfoliosHook.setCurrentPortfolioId(newPf.id);
          }}
        />
      )}

      {modals.showAlertModal && (
        <NewPriceAlertModal
          trackedStocks={trackedHook.trackedStocks}
          onClose={modals.closeAlertModal}
          onCreated={alert => {
            alertsHook.setAlerts(prev => [
              ...prev,
              normalizeAlert(alert, trackedHook.trackedStocksRef.current),
            ]);
          }}
        />
      )}
    </div>
  );
}
