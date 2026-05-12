import { useState, useEffect, useRef, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api';
import { useLivePrices } from '../hooks/useLivePrices';
import { useAlertWebSocket } from '../hooks/useAlertWebSocket';
import { usePortfolioWebSocket } from '../hooks/usePortfolioWebSocket';
import Sidebar from '../components/sidebar/Sidebar';
import DashboardHeader from '../components/header/DashboardHeader';
import SummaryCards from '../components/summary/SummaryCards';
import HoldingsTable from '../components/holdings/HoldingsTable';
import RightPanel from '../components/rightpanel/RightPanel';
import RecordTransactionModal from '../components/modals/RecordTransactionModal';
import FindTickerModal from '../components/modals/FindTickerModal';
import NewPortfolioModal from '../components/modals/NewPortfolioModal';
import NewPriceAlertModal from '../components/modals/NewPriceAlertModal';
import CashTransactionModal from '../components/modals/CashTransactionModal';
import EmptyPortfolioState from '../components/EmptyPortfolioState';
import PortfolioTable from '../components/portfolio/PortfolioTable';
import MarketView from '../components/MarketView';
import GlobalTransactionsView from '../components/GlobalTransactionsView';

export default function DashboardPage() {
  const { userId, token } = useAuth();

  const [activeNav, setActiveNav]                             = useState('dashboard');
  const [portfolios, setPortfolios]                           = useState([]);
  const [viewingPortfolioId, setViewingPortfolioId]           = useState(null);
  const [portfolioPerformanceMap, setPortfolioPerformanceMap] = useState({});
  const [enrichedHoldings, setEnrichedHoldings]               = useState([]);
  const [alerts, setAlerts]                                   = useState([]);
  const [transactions, setTransactions]                       = useState([]);
  const [performance, setPerformance]                         = useState(null);
  const [companyNames, setCompanyNames]                       = useState({});
  const [sparklineData, setSparklineData]                     = useState([]);

  const [showTxModal, setShowTxModal]         = useState(false);
  const [showTickerModal, setShowTickerModal] = useState(false);
  const [showPfModal, setShowPfModal]         = useState(false);
  const [showAlertModal, setShowAlertModal]   = useState(false);
  const [showCashModal, setShowCashModal]     = useState(false);

  const priceHistoryRef       = useRef([]);
  const lastPushRef           = useRef(0);
  const trackedStocksRef      = useRef({});
  const companyNameCache      = useRef({});
  const fetchingTickers       = useRef(new Set());
  const sessionBaselineRef    = useRef(null);
  const viewingPortfolioIdRef = useRef(null);

  const { prices: livePrices, dayChangePct, lastUpdatedAt } = useLivePrices(token);
  const { triggeredAlerts } = useAlertWebSocket(userId, token);
  const { portfolioPerf: livePortfolioPerf } = usePortfolioWebSocket(viewingPortfolioId, token);

  // ── Normalizers ───────────────────────────────────────────────────

  function normalizeAlert(a, trackedMap) {
    return {
      ...a,
      id:          a.priceAlertId ?? a.alertId,
      ticker:      a.ticker ?? trackedMap[a.assetId]?.ticker ?? '—',
      targetPrice: Number(a.targetPrice ?? 0),
    };
  }

  function normalizeTx(tx, trackedMap) {
    return {
      id:              tx.transactionId,
      transactionType: tx.type,
      ticker:          trackedMap[tx.assetId]?.ticker ?? '—',
      quantity:        tx.quantity,
      price:           tx.pricePerUnit,
    };
  }

  function buildEnrichedHoldings(rawHoldings, perf, trackedMap) {
    const perfMap = {};
    (perf.holdings ?? []).forEach(h => { perfMap[h.holdingId] = h; });
    return rawHoldings.map(h => ({
      holdingId:          h.holdingId,
      assetId:            h.assetId,
      quantity:           Number(h.quantity),
      avgCost:            Number(h.avgCost),
      ticker:             trackedMap[h.assetId]?.ticker ?? '—',
      currentMarketPrice: Number(perfMap[h.holdingId]?.currentMarketPrice ?? 0),
      absoluteChange:     Number(perfMap[h.holdingId]?.absoluteChange ?? 0),
      percentageChange:   Number(perfMap[h.holdingId]?.percentageChange ?? 0),
    }));
  }

  // ── Company name fetch ────────────────────────────────────────────

  function fetchCompanyNames(holdings) {
    const tickers = [...new Set(holdings.map(h => h.ticker).filter(t => t && t !== '—'))];
    const fromCache = {};
    const toFetch   = [];

    tickers.forEach(ticker => {
      if (companyNameCache.current[ticker]) {
        fromCache[ticker] = companyNameCache.current[ticker];
      } else if (!fetchingTickers.current.has(ticker)) {
        toFetch.push(ticker);
      }
    });

    if (Object.keys(fromCache).length > 0) {
      setCompanyNames(prev => ({ ...prev, ...fromCache }));
    }

    toFetch.forEach(ticker => {
      fetchingTickers.current.add(ticker);
      api.market.getProfile(ticker)
        .then(p => {
          companyNameCache.current[ticker] = p.name;
          fetchingTickers.current.delete(ticker);
          setCompanyNames(prev => ({ ...prev, [ticker]: p.name }));
        })
        .catch(() => fetchingTickers.current.delete(ticker));
    });
  }

  // ── Initial load ──────────────────────────────────────────────────

  useEffect(() => {
    if (!userId) return;

    trackedStocksRef.current = {};
    setViewingPortfolioId(null);
    setPortfolios([]);
    setAlerts([]);
    setEnrichedHoldings([]);
    setTransactions([]);
    setPerformance(null);
    setPortfolioPerformanceMap({});
    setCompanyNames({});

    let stale = false;

    Promise.all([
      api.portfolios.getAll(),
      api.market.getTrackedStocks(),
      api.alerts.getAll(),
    ]).then(([pfs, tracked, rawAlerts]) => {
      if (stale) return;
      const trackedMap = {};
      tracked.forEach(s => { trackedMap[s.assetId] = s; });
      trackedStocksRef.current = trackedMap;
      setPortfolios(pfs);
      setAlerts(rawAlerts.map(a => normalizeAlert(a, trackedMap)));

      if (pfs.length > 0) {
        Promise.allSettled(pfs.map(p => api.portfolios.getPerformance(p.id)))
          .then(results => {
            if (stale) return;
            const map = {};
            pfs.forEach((p, i) => {
              const r = results[i];
              if (r.status !== 'fulfilled') return;
              const perf    = r.value;
              const invested = Number(perf?.totalInvestedValue ?? 0);
              const value    = Number(perf?.portfolioValue ?? 0);
              map[p.id] = {
                portfolioValue: value,
                returnPct:      invested ? ((value - invested) / invested) * 100 : 0,
                absoluteReturn: value - invested,
              };
            });
            setPortfolioPerformanceMap(map);
          });
      }
    }).catch(console.error);

    return () => { stale = true; };
  }, [userId]);

  // Keep ref in sync with state for async callbacks.
  useEffect(() => { viewingPortfolioIdRef.current = viewingPortfolioId; }, [viewingPortfolioId]);

  // Clear portfolio-specific data when returning to the portfolio list.
  useEffect(() => {
    if (viewingPortfolioId !== null) return;
    setEnrichedHoldings([]);
    setTransactions([]);
    setPerformance(null);
    priceHistoryRef.current    = [];
    lastPushRef.current        = 0;
    sessionBaselineRef.current = null;
    setSparklineData([]);
  }, [viewingPortfolioId]);

  // ── Per-portfolio fetch ────────────────────────────────────────────

  useEffect(() => {
    if (!userId || !viewingPortfolioId) return;

    setEnrichedHoldings([]);
    setTransactions([]);
    setPerformance(null);
    priceHistoryRef.current    = [];
    lastPushRef.current        = 0;
    sessionBaselineRef.current = null;
    setSparklineData([]);

    let stale = false;

    Promise.all([
      api.portfolios.getPerformance(viewingPortfolioId),
      api.holdings.getAll(viewingPortfolioId),
      api.transactions.getPortfolio(viewingPortfolioId, 10),
    ]).then(([perf, rawHoldings, rawTxs]) => {
      if (stale) return;
      const trackedMap = trackedStocksRef.current;
      setPerformance(perf);
      const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
      setEnrichedHoldings(enriched);
      setTransactions((rawTxs.content ?? rawTxs ?? []).map(tx => normalizeTx(tx, trackedMap)));
      fetchCompanyNames(enriched);

      const initialValue = Number(perf.portfolioValue ?? 0);
      if (initialValue > 0) {
        priceHistoryRef.current    = [initialValue];
        lastPushRef.current        = Date.now();
        sessionBaselineRef.current = initialValue;
        setSparklineData([initialValue]);
      }
    }).catch(console.error);

    return () => { stale = true; };
  }, [userId, viewingPortfolioId]);

  // ── Apply live portfolio performance from WebSocket ───────────────

  useEffect(() => {
    if (!livePortfolioPerf) return;
    setPerformance(livePortfolioPerf);
    // Sync cashBalance in portfolios state so CashCard stays accurate without a REST poll.
    if (livePortfolioPerf.cashBalance != null) {
      setPortfolios(prev => prev.map(p =>
        p.id === livePortfolioPerf.portfolioId
          ? { ...p, cashBalance: livePortfolioPerf.cashBalance }
          : p
      ));
    }
  }, [livePortfolioPerf]);

  // ── Auto-splice triggered alerts from active list ─────────────────

  useEffect(() => {
    if (!triggeredAlerts.length) return;
    const firedIds = new Set(
      triggeredAlerts.map(a => a.priceAlertId ?? a.alertId).filter(Boolean)
    );
    setAlerts(prev => prev.filter(a => !firedIds.has(a.id)));
  }, [triggeredAlerts]);

  // ── Throttled sparkline update ─────────────────────────────────────

  const livePortfolioValue = useMemo(() =>
    enrichedHoldings.reduce((sum, h) => {
      const price = livePrices.get(h.ticker) ?? h.currentMarketPrice ?? 0;
      return sum + price * h.quantity;
    }, 0),
  [enrichedHoldings, livePrices]);

  useEffect(() => {
    if (enrichedHoldings.length === 0) return;
    const now = Date.now();
    if (now - lastPushRef.current < 2000) return;
    lastPushRef.current = now;

    if (sessionBaselineRef.current === null) {
      sessionBaselineRef.current = livePortfolioValue;
    }

    const updated = [...priceHistoryRef.current.slice(-59), livePortfolioValue];
    priceHistoryRef.current = updated;
    setSparklineData([...updated]);
  }, [livePortfolioValue, enrichedHoldings.length]);

  // ── Normalize triggered alerts ────────────────────────────────────

  const normalizedTriggeredAlerts = useMemo(() =>
    triggeredAlerts.map(a => ({
      ...a,
      id:           a.priceAlertId ?? a.alertId,
      ticker:       a.ticker ?? trackedStocksRef.current[a.assetId]?.ticker ?? '—',
      targetPrice:  Number(a.targetPrice ?? 0),
      currentPrice: Number(a.currentPrice ?? 0),
    })),
  [triggeredAlerts]);

  // ── Derived summary values ─────────────────────────────────────────

  const currentPortfolio = portfolios.find(p => p.id === viewingPortfolioId);
  const cash             = Number(currentPortfolio?.cashBalance ?? 0);
  const totalValue       = livePortfolioValue || Number(performance?.portfolioValue ?? 0);
  const totalInvested    = Number(performance?.totalInvestedValue ?? 0);
  const absoluteReturn   = totalInvested ? totalValue - totalInvested : 0;
  const returnPct        = totalInvested ? (absoluteReturn / totalInvested) * 100 : 0;

  const sessionChange = sessionBaselineRef.current !== null
    ? livePortfolioValue - sessionBaselineRef.current
    : 0;

  const holdingsWithNames = useMemo(() =>
    enrichedHoldings.map(h => ({
      ...h,
      companyName: companyNames[h.ticker] ?? '',
    })),
  [enrichedHoldings, companyNames]);

  // ── Navigation ────────────────────────────────────────────────────

  function handleNavClick(navId) {
    setActiveNav(navId);
    if (navId !== 'dashboard') setViewingPortfolioId(null);
  }

  function handleSelectPortfolio(id) {
    setViewingPortfolioId(id);
    setActiveNav('dashboard');
  }

  // ── Refetch helpers ────────────────────────────────────────────────

  async function refetchPortfolioData() {
    if (!viewingPortfolioId) return;
    const capturedId = viewingPortfolioId;
    const [perf, rawHoldings, rawTxs, updatedPortfolio] = await Promise.all([
      api.portfolios.getPerformance(capturedId),
      api.holdings.getAll(capturedId),
      api.transactions.getPortfolio(capturedId, 10),
      api.portfolios.getById(capturedId),
    ]);
    if (viewingPortfolioIdRef.current !== capturedId) return;
    const trackedMap = trackedStocksRef.current;
    setPerformance(perf);
    setPortfolios(prev => prev.map(p => p.id === capturedId ? updatedPortfolio : p));
    const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
    setEnrichedHoldings(enriched);
    setTransactions((rawTxs.content ?? rawTxs ?? []).map(tx => normalizeTx(tx, trackedMap)));
    fetchCompanyNames(enriched);
  }

  async function handleDeleteAlert(alertId) {
    await api.alerts.remove(alertId);
    setAlerts(prev => prev.filter(a => a.id !== alertId));
  }

  // ── Computed header title ──────────────────────────────────────────

  const headerTitle = activeNav === 'market'       ? 'Market'
    : activeNav === 'transactions'                  ? 'Transactions'
    : viewingPortfolioId                            ? (currentPortfolio?.name ?? 'Portfolio')
    : 'Dashboard';

  const inPortfolioDetail = activeNav === 'dashboard' && viewingPortfolioId !== null;

  return (
    <div className={`dashboard-layout${inPortfolioDetail ? '' : ' no-rail'}`}>
      <Sidebar
        activeNav={activeNav}
        setActiveNav={handleNavClick}
        portfolios={portfolios}
        currentPortfolioId={viewingPortfolioId}
        setCurrentPortfolioId={handleSelectPortfolio}
        onNewPortfolio={() => setShowPfModal(true)}
      />

      <main className="main">
        <DashboardHeader
          portfolioName={headerTitle}
          onFindTicker={inPortfolioDetail || activeNav === 'market' ? () => setShowTickerModal(true) : undefined}
          onNewAlert={activeNav === 'dashboard' && !viewingPortfolioId ? () => setShowAlertModal(true) : undefined}
          onDeposit={inPortfolioDetail ? () => setShowCashModal(true) : undefined}
          onRecordTransaction={activeNav !== 'transactions' ? () => {
            if (!viewingPortfolioId) { setShowPfModal(true); return; }
            setShowTxModal(true);
          } : undefined}
        />

        {activeNav === 'market' && (
          <MarketView
            trackedStocks={Object.values(trackedStocksRef.current)}
            livePrices={livePrices}
            dayChangePct={dayChangePct}
            lastUpdatedAt={lastUpdatedAt}
          />
        )}

        {activeNav === 'transactions' && (
          <GlobalTransactionsView trackedStocksRef={trackedStocksRef} />
        )}

        {activeNav === 'dashboard' && viewingPortfolioId === null && (
          portfolios.length === 0
            ? <EmptyPortfolioState onCreatePortfolio={() => setShowPfModal(true)} />
            : <PortfolioTable
                portfolios={portfolios}
                performanceMap={portfolioPerformanceMap}
                onSelect={handleSelectPortfolio}
                onNew={() => setShowPfModal(true)}
              />
        )}

        {inPortfolioDetail && (
          <>
            <SummaryCards
              totalValue={totalValue}
              sessionChange={sessionChange}
              priceHistory={sparklineData}
              cash={cash}
              currencyCode={currentPortfolio?.currencyCode}
              returnPct={returnPct}
              absoluteReturn={absoluteReturn}
              portfolioStatus={currentPortfolio?.status}
              portfolioCreatedAt={currentPortfolio?.createdAt}
            />
            <HoldingsTable
              holdings={holdingsWithNames}
              livePrices={livePrices}
              dayChangePct={dayChangePct}
              lastUpdated={lastUpdatedAt ? lastUpdatedAt.toLocaleTimeString() : null}
            />
          </>
        )}
      </main>

      {inPortfolioDetail && (
        <RightPanel
          triggeredAlerts={normalizedTriggeredAlerts}
          activeAlerts={alerts}
          transactions={transactions}
          onDeleteAlert={handleDeleteAlert}
        />
      )}

      {showTxModal && (
        <RecordTransactionModal
          portfolioId={viewingPortfolioId}
          onClose={() => setShowTxModal(false)}
          onSuccess={async ({ transactionType, ticker, quantity }) => {
            const entry = Object.values(trackedStocksRef.current).find(s => s.ticker === ticker);
            if (!entry) throw new Error(`"${ticker}" is not tracked. Use Find Ticker to add it first.`);
            await api.transactions.record(viewingPortfolioId, {
              assetId:     entry.assetId,
              portfolioId: viewingPortfolioId,
              executedAt:  new Date().toISOString(),
              quantity,
              type:        transactionType,
            });
            refetchPortfolioData().catch(console.error);
          }}
        />
      )}

      {showTickerModal && (
        <FindTickerModal onClose={() => setShowTickerModal(false)} />
      )}

      {showAlertModal && (
        <NewPriceAlertModal
          trackedStocks={Object.values(trackedStocksRef.current)}
          onClose={() => setShowAlertModal(false)}
          onCreated={(newAlert) => {
            const trackedMap = trackedStocksRef.current;
            setAlerts(prev => [...prev, normalizeAlert(newAlert, trackedMap)]);
          }}
        />
      )}

      {showPfModal && (
        <NewPortfolioModal
          onClose={() => setShowPfModal(false)}
          onCreate={async (body) => {
            const newPf = await api.portfolios.create(body);
            const pfs   = await api.portfolios.getAll();
            setPortfolios(pfs);
            if (newPf?.id) handleSelectPortfolio(newPf.id);
          }}
        />
      )}

      {showCashModal && (
        <CashTransactionModal
          portfolioId={viewingPortfolioId}
          onClose={() => setShowCashModal(false)}
          onSuccess={() => refetchPortfolioData().catch(console.error)}
        />
      )}
    </div>
  );
}
