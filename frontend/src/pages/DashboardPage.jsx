import { useState, useEffect, useRef, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api';
import { useLivePrices } from '../hooks/useLivePrices';
import { useAlertWebSocket } from '../hooks/useAlertWebSocket';
import Sidebar from '../components/sidebar/Sidebar';
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
import { usePortfolioWebSocket } from '../hooks/usePortfolioWebSocket';

export default function DashboardPage() {
  const { userId, token } = useAuth();

  const [activeNav, setActiveNav]                   = useState('dashboard');
  const [dashboardView, setDashboardView]           = useState('list'); // 'list' | 'detail'
  const [portfolios, setPortfolios]                 = useState([]);
  const [currentPortfolioId, setCurrentPortfolioId] = useState(null);
  const [enrichedHoldings, setEnrichedHoldings]     = useState([]);
  const [alerts, setAlerts]                         = useState([]);
  const [transactions, setTransactions]             = useState([]);
  const [trackedCount, setTrackedCount]             = useState(0);
  const [trackedStocks, setTrackedStocks]           = useState([]);
  const [performance, setPerformance]               = useState(null);
  const [performanceMap, setPerformanceMap]         = useState({});
  const [companyProfiles, setCompanyProfiles]       = useState({});
  const [sparklineData, setSparklineData]           = useState([]);

  const [txRefreshKey, setTxRefreshKey]       = useState(0);

  const [showTxModal, setShowTxModal]         = useState(false);
  const [showCashModal, setShowCashModal]     = useState(false);
  const [showTickerModal, setShowTickerModal] = useState(false);
  const [showPfModal, setShowPfModal]         = useState(false);
  const [showAlertModal, setShowAlertModal]   = useState(false);

  const priceHistoryRef       = useRef([]);
  const lastPushRef           = useRef(0);
  const trackedStocksRef      = useRef({});
  const companyNameCache      = useRef({});
  const fetchingTickers       = useRef(new Set());
  const sessionBaselineRef    = useRef(null);
  const currentPortfolioIdRef = useRef(null);

  const { prices: livePrices, dayChangePct, lastUpdatedAt } = useLivePrices(token);
  const { triggeredAlerts } = useAlertWebSocket(userId, token);
  const { portfolioPerf } = usePortfolioWebSocket(currentPortfolioId, token);

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

  function fetchCompanyProfiles(tickers) {
    const unique = [...new Set(tickers.filter(t => t && t !== '—'))];
    const fromCache = {};
    const toFetch   = [];

    unique.forEach(ticker => {
      if (companyNameCache.current[ticker]) {
        fromCache[ticker] = companyNameCache.current[ticker];
      } else if (!fetchingTickers.current.has(ticker)) {
        toFetch.push(ticker);
      }
    });

    if (Object.keys(fromCache).length > 0) {
      setCompanyProfiles(prev => ({ ...prev, ...fromCache }));
    }

    toFetch.forEach(ticker => {
      fetchingTickers.current.add(ticker);
      api.market.getProfile(ticker)
        .then(p => {
          const profile = { name: p.name ?? '', industry: p.industry ?? '' };
          companyNameCache.current[ticker] = profile;
          fetchingTickers.current.delete(ticker);
          setCompanyProfiles(prev => ({ ...prev, [ticker]: profile }));
        })
        .catch(() => fetchingTickers.current.delete(ticker));
    });
  }

  useEffect(() => {
    if (!userId) return;

    trackedStocksRef.current = {};
    setCurrentPortfolioId(null);
    setPortfolios([]);
    setAlerts([]);
    setEnrichedHoldings([]);
    setTransactions([]);
    setPerformance(null);
    setTrackedCount(0);
    setCompanyProfiles({});

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
      setTrackedStocks(tracked);
      setTrackedCount(tracked.length);
      setAlerts(rawAlerts.map(a => normalizeAlert(a, trackedMap)));
      if (pfs.length > 0) setCurrentPortfolioId(pfs[0].id);
      fetchCompanyProfiles(tracked.map(s => s.ticker));
    }).catch(console.error);

    return () => { stale = true; };
  }, [userId]);

  // Keep ref in sync so async callbacks can detect a portfolio switch.
  useEffect(() => { currentPortfolioIdRef.current = currentPortfolioId; }, [currentPortfolioId]);

  useEffect(() => {
    if (!userId || !currentPortfolioId) return;

    // Clear stale portfolio data immediately
    setEnrichedHoldings([]);
    setTransactions([]);
    setPerformance(null);
    priceHistoryRef.current   = [];
    lastPushRef.current       = 0;
    sessionBaselineRef.current = null;
    setSparklineData([]);

    let stale = false;

    Promise.all([
      api.portfolios.getPerformance(currentPortfolioId),
      api.holdings.getAll(currentPortfolioId),
      api.transactions.getPortfolio(currentPortfolioId, 10),
    ]).then(([perf, rawHoldings, rawTxs]) => {
      if (stale) return;
      const trackedMap = trackedStocksRef.current;
      setPerformance(perf);
      setPerformanceMap(prev => ({ ...prev, [currentPortfolioId]: perf }));
      const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
      setEnrichedHoldings(enriched);
      // Response is a Spring Page<>; content holds the actual array.
      setTransactions((rawTxs.content ?? rawTxs ?? []).map(tx => normalizeTx(tx, trackedMap)));
      fetchCompanyProfiles(enriched.map(h => h.ticker));

      // Fix #3: seed sparkline immediately with API-derived portfolio value
      // so the chart isn't empty while waiting for the first WS tick.
      const initialValue = Number(perf.portfolioValue ?? 0);
      if (initialValue > 0) {
        priceHistoryRef.current    = [initialValue];
        lastPushRef.current        = Date.now();
        sessionBaselineRef.current = initialValue;
        setSparklineData([initialValue]);
      }
    }).catch(console.error);

    return () => { stale = true; };
  }, [userId, currentPortfolioId]);

  // ── Real-time portfolio value via WebSocket ────────────────────────

  useEffect(() => {
    if (!portfolioPerf) return;
    setPerformance(portfolioPerf);
    setPortfolios(prev => prev.map(p =>
      p.id === currentPortfolioId ? { ...p, cashBalance: portfolioPerf.cashBalance } : p
    ));
    const perfMap = {};
    (portfolioPerf.holdings ?? []).forEach(h => { perfMap[h.holdingId] = h; });
    setEnrichedHoldings(prev => prev.map(h => {
      const p = perfMap[h.holdingId];
      if (!p) return h;
      return {
        ...h,
        currentMarketPrice: Number(p.currentMarketPrice),
        absoluteChange:     Number(p.absoluteChange),
        percentageChange:   Number(p.percentageChange),
      };
    }));
  }, [portfolioPerf]);

  // ── Auto-splice triggered alerts from active list ─────────────────
  // Fix #7: simplified — normalized alerts always have `id` set.

  useEffect(() => {
    if (!triggeredAlerts.length) return;
    const firedIds = new Set(
      triggeredAlerts.map(a => a.priceAlertId ?? a.alertId).filter(Boolean)
    );
    setAlerts(prev => prev.filter(a => !firedIds.has(a.id)));
  }, [triggeredAlerts]);

  // ── Throttled sparkline update (2 s min, cap 60 entries) ──────────

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

    // Fix #8: set the baseline once on the first WS-driven push,
    // not from sparklineData[0] which shifts as the array slides.
    if (sessionBaselineRef.current === null) {
      sessionBaselineRef.current = livePortfolioValue;
    }

    const updated = [...priceHistoryRef.current.slice(-59), livePortfolioValue];
    priceHistoryRef.current = updated;
    setSparklineData([...updated]);
  }, [livePortfolioValue, enrichedHoldings.length]);

  // ── Fix #6 + Bug #9: normalize triggered alerts before rendering ──────
  // Ensures numeric prices and resolves ticker from WS payload or tracked map.

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

  const currentPortfolio = portfolios.find(p => p.id === currentPortfolioId);
  const currencyCode     = currentPortfolio?.currencyCode ?? 'USD';
  const cash             = Number(currentPortfolio?.cashBalance ?? 0);
  const totalValue       = livePortfolioValue || Number(performance?.portfolioValue ?? 0);
  const totalInvested    = Number(performance?.totalInvestedValue ?? 0);
  const absoluteReturn   = totalInvested ? totalValue - totalInvested : 0;
  const returnPct        = totalInvested ? (absoluteReturn / totalInvested) * 100 : 0;

  // Fix #8: use stable baseline ref instead of sparklineData[0]
  const sessionChange = sessionBaselineRef.current !== null
    ? livePortfolioValue - sessionBaselineRef.current
    : 0;

  // Fix #14: memoize to avoid re-rendering HoldingsTable on every parent render
  const holdingsWithNames = useMemo(() =>
    enrichedHoldings.map(h => ({
      ...h,
      companyName: companyProfiles[h.ticker]?.name ?? '',
    })),
  [enrichedHoldings, companyProfiles]);

  // ── Refetch helpers ────────────────────────────────────────────────

  async function refetchPortfolioData() {
    if (!currentPortfolioId) return;
    // Capture id at call time; compare after await to detect a portfolio switch.
    const capturedId = currentPortfolioId;
    const [perf, rawHoldings, rawTxs] = await Promise.all([
      api.portfolios.getPerformance(capturedId),
      api.holdings.getAll(capturedId),
      api.transactions.getPortfolio(capturedId, 10),
    ]);
    if (currentPortfolioIdRef.current !== capturedId) return;
    const trackedMap = trackedStocksRef.current;
    setPerformance(perf);
    setPerformanceMap(prev => ({ ...prev, [capturedId]: perf }));
    const enriched = buildEnrichedHoldings(rawHoldings, perf, trackedMap);
    setEnrichedHoldings(enriched);
    setTransactions((rawTxs.content ?? rawTxs ?? []).map(tx => normalizeTx(tx, trackedMap)));
    fetchCompanyProfiles(enriched.map(h => h.ticker));
  }

  async function handleDeleteAlert(alertId) {
    await api.alerts.remove(alertId);
    setAlerts(prev => prev.filter(a => a.id !== alertId));
  }

  function handleUpdateAlert(updated) {
    setAlerts(prev => prev.map(a => a.id === updated.id ? { ...a, ...updated } : a));
  }

  async function handleSelectPortfolio(id) {
    setCurrentPortfolioId(id);
    setDashboardView('detail');
    try {
      const fresh = await api.portfolios.getById(id);
      setPortfolios(prev => prev.map(p => p.id === fresh.id ? { ...p, ...fresh } : p));
    } catch (e) {
      console.error(e);
    }
  }

  async function handleRemoveTracking(ticker) {
    try {
      await api.market.removeTracking(ticker);
      const next = trackedStocks.filter(s => s.ticker !== ticker);
      setTrackedStocks(next);
      setTrackedCount(next.length);
    } catch (e) {
      console.error('Failed to untrack', ticker, e);
    }
  }

  // Reset to list view whenever the user navigates away from dashboard
  useEffect(() => {
    if (activeNav !== 'dashboard') setDashboardView('list');
  }, [activeNav]);

  return (
    <div className="dashboard-layout">
      <Sidebar
        activeNav={activeNav}
        setActiveNav={setActiveNav}
        portfolios={portfolios}
        currentPortfolioId={currentPortfolioId}
        setCurrentPortfolioId={handleSelectPortfolio}
        alertCount={alerts.length || undefined}
        onNewPortfolio={() => setShowPfModal(true)}
      />

      <main className="main">
        <DashboardHeader
          title={
            activeNav === 'market'       ? 'Market'       :
            activeNav === 'transactions' ? 'Transactions' :
            dashboardView === 'detail'   ? currentPortfolio?.name : undefined
          }
          onBack={activeNav === 'dashboard' && dashboardView === 'detail' ? () => setDashboardView('list') : undefined}
          onFindTicker={dashboardView === 'detail' || activeNav === 'market' ? () => setShowTickerModal(true) : undefined}
          onNewAlert={dashboardView === 'detail' || activeNav === 'market' ? () => setShowAlertModal(true) : undefined}
          onDeposit={dashboardView === 'detail' ? () => setShowCashModal(true) : undefined}
          onRecordTransaction={dashboardView === 'detail' ? () => {
            if (!currentPortfolioId) { setShowPfModal(true); return; }
            setShowTxModal(true);
          } : undefined}
        />

        {activeNav === 'dashboard' && dashboardView === 'list' && (
          <PortfolioTable
            portfolios={portfolios}
            performanceMap={performanceMap}
            onSelect={handleSelectPortfolio}
            onNew={() => setShowPfModal(true)}
          />
        )}

        {activeNav === 'dashboard' && dashboardView === 'detail' && (
          <>
            <SummaryCards
              totalValue={totalValue}
              sessionChange={sessionChange}
              priceHistory={sparklineData}
              cash={cash}
              currencyCode={currencyCode}
              portfolioCount={portfolios.length}
              returnPct={returnPct}
              absoluteReturn={absoluteReturn}
              trackedCount={trackedCount}
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
            trackedStocks={trackedStocks}
            livePrices={livePrices}
            lastUpdatedAt={lastUpdatedAt}
            companyProfiles={companyProfiles}
            onRemove={handleRemoveTracking}
          />
        )}

        {activeNav === 'transactions' && (
          <GlobalTransactionsView trackedStocksRef={trackedStocksRef} portfolios={portfolios} companyProfiles={companyProfiles} refreshKey={txRefreshKey} />
        )}
      </main>

      <RightPanel
        triggeredAlerts={normalizedTriggeredAlerts}
        activeAlerts={alerts}
        transactions={transactions}
        onDeleteAlert={handleDeleteAlert}
        onUpdateAlert={handleUpdateAlert}
      />

      {showCashModal && (
        <CashTransactionModal
          portfolioId={currentPortfolioId}
          onSuccess={() => { setTxRefreshKey(k => k + 1); refetchPortfolioData(); }}
          onClose={() => setShowCashModal(false)}
        />
      )}

      {showTxModal && (
        <RecordTransactionModal
          portfolioId={currentPortfolioId}
          onClose={() => setShowTxModal(false)}
          onSuccess={async ({ transactionType, ticker, quantity }) => {
            // Map modal fields → TransactionRequest shape.
            // Backend uses assetId + executedAt to look up price itself;
            // the modal's price input is not sent to the backend.
            const entry = Object.values(trackedStocksRef.current).find(s => s.ticker === ticker);
            if (!entry) throw new Error(`"${ticker}" is not tracked. Use Find Ticker to add it first.`);
            await api.transactions.record(currentPortfolioId, {
              assetId:     entry.assetId,
              portfolioId: currentPortfolioId,
              executedAt:  new Date().toISOString(),
              quantity,
              type:        transactionType,
            });
            setTxRefreshKey(k => k + 1);
            refetchPortfolioData().catch(console.error);
          }}
        />
      )}

      {showTickerModal && (
        <FindTickerModal onClose={() => setShowTickerModal(false)} />
      )}

      {showPfModal && (
        <NewPortfolioModal
          onClose={() => setShowPfModal(false)}
          onCreate={async (body) => {
            // Fix #12: rely on the create response id; no array-position fallback.
            const newPf = await api.portfolios.create(body);
            const pfs   = await api.portfolios.getAll();
            setPortfolios(pfs);
            if (newPf?.id) setCurrentPortfolioId(newPf.id);
          }}
        />
      )}

      {showAlertModal && (
        <NewPriceAlertModal
          trackedStocks={trackedStocks}
          onClose={() => setShowAlertModal(false)}
          onCreated={alert => {
            setAlerts(prev => [...prev, normalizeAlert(alert, trackedStocksRef.current)]);
          }}
        />
      )}
    </div>
  );
}
