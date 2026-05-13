import { useState, useEffect, Fragment } from 'react';
import { api } from '../api';

const TABS = ['Trades', 'Cash Flow'];

export default function GlobalTransactionsView({ trackedStocksRef, portfolios = [], companyProfiles = {}, refreshKey = 0 }) {
  const [tab, setTab]                         = useState('Trades');
  const [trades, setTrades]                   = useState([]);
  const [cashTxs, setCashTxs]                 = useState([]);
  const [loadingTrades, setLoadingTrades]     = useState(true);
  const [loadingCash, setLoadingCash]         = useState(false);
  const [cashPortfolioFilter, setCashPortfolioFilter] = useState(null);

  function normalize(res) {
    if (!res) return [];
    return Array.isArray(res) ? res : (res.content ?? []);
  }

  useEffect(() => {
    let ignore = false;
    setLoadingTrades(true);
    api.transactions.getAll(50)
      .then(res => { if (!ignore) setTrades(normalize(res)); })
      .catch(console.error)
      .finally(() => { if (!ignore) setLoadingTrades(false); });
    return () => { ignore = true; };
  }, [refreshKey]);

  useEffect(() => {
    if (tab !== 'Cash Flow') return;
    let ignore = false;
    setLoadingCash(true);
    const req = cashPortfolioFilter
      ? api.cashTransactions.getAll(cashPortfolioFilter, 50)
      : api.cashTransactions.getAllGlobal(50);
    req
      .then(res => { if (!ignore) setCashTxs(normalize(res)); })
      .catch(console.error)
      .finally(() => { if (!ignore) setLoadingCash(false); });
    return () => { ignore = true; };
  }, [tab, cashPortfolioFilter, refreshKey]);

  return (
    <div className="panel">
      <div className="phead">
        <div>
          <h2>Transactions</h2>
          <div className="meta">
            {tab === 'Trades'
              ? (loadingTrades ? '…' : `${trades.length} recent trades`)
              : (loadingCash ? '…' : `${cashTxs.length} cash movements`)}
          </div>
        </div>
        <div>
          <div className="filter">
            {TABS.map(t => (
              <button key={t} className={`chip${tab === t ? ' on' : ''}`} onClick={() => setTab(t)}>{t}</button>
            ))}
          </div>
          {tab === 'Cash Flow' && portfolios.length > 0 && (
            <div className="filter" style={{ marginTop: 6 }}>
              <button
                className={`chip${cashPortfolioFilter === null ? ' on' : ''}`}
                onClick={() => setCashPortfolioFilter(null)}
              >All</button>
              {portfolios.map(p => (
                <button
                  key={p.id}
                  className={`chip${cashPortfolioFilter === p.id ? ' on' : ''}`}
                  onClick={() => setCashPortfolioFilter(p.id)}
                >{p.name}</button>
              ))}
            </div>
          )}
        </div>
      </div>

      {tab === 'Trades' && (
        <div className="tx-full-grid">
          <div className="gh">Type</div>
          <div className="gh">Ticker</div>
          <div className="gh r">Qty</div>
          <div className="gh r">Total</div>
          <div className="gh">Currency</div>
          <div className="gh">Portfolio</div>
          <div className="gh">Date</div>
          {loadingTrades ? (
            <div style={{ gridColumn: '1 / -1', padding: '24px 0', color: 'var(--ink-3)', fontSize: '13px' }}>Loading…</div>
          ) : trades.length === 0 ? (
            <div style={{ gridColumn: '1 / -1', padding: '24px 0', color: 'var(--ink-3)', fontSize: '13px' }}>No trades yet.</div>
          ) : (
            trades.map((tx, i) => {
              const isBuy    = tx.type === 'BUY';
              const stocks   = trackedStocksRef?.current ?? {};
              const stock    = Object.values(stocks).find(s => s.assetId === tx.assetId);
              const ticker   = stock?.ticker ?? '—';
              const name     = companyProfiles[ticker]?.name ?? '';
              const total    = (Number(tx.quantity) || 0) * (Number(tx.pricePerUnit) || 0);
              const pf       = portfolios.find(p => p.id === tx.portfolioId);
              const pfName   = pf?.name ?? `Portfolio ${tx.portfolioId}`;
              const dateTime = tx.executedAt ? new Date(tx.executedAt).toLocaleString() : '—';
              return (
                <Fragment key={tx.transactionId ?? i}>
                  <div className="gc"><span className={`tx-key ${isBuy ? 'b' : 's'}`}>{tx.type}</span></div>
                  <div className="gc">
                    <span className="sym" style={{ fontWeight: 500 }}>{ticker}</span>
                    {name && <div className="holding-name">{name}</div>}
                  </div>
                  <div className="gc r num">{tx.quantity}</div>
                  <div className="gc r num">
                    ${total.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{tx.currencyCode ?? '—'}</div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-2)' }}>{pfName}</div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{dateTime}</div>
                </Fragment>
              );
            })
          )}
        </div>
      )}

      {tab === 'Cash Flow' && (
        <div className="tx-full-grid" style={{ gridTemplateColumns: '1fr 2fr 1fr 1fr' }}>
          <div className="gh">Type</div>
          <div className="gh">Portfolio</div>
          <div className="gh r">Amount</div>
          <div className="gh">Date</div>
          {loadingCash ? (
            <div style={{ gridColumn: '1 / -1', padding: '24px 0', color: 'var(--ink-3)', fontSize: '13px' }}>Loading…</div>
          ) : cashTxs.length === 0 ? (
            <div style={{ gridColumn: '1 / -1', padding: '24px 0', color: 'var(--ink-3)', fontSize: '13px' }}>No cash movements yet.</div>
          ) : (
            cashTxs.map((tx, i) => {
              const isDeposit = tx.type === 'DEPOSIT';
              const pf        = portfolios.find(p => p.id === tx.portfolioId);
              const pfName    = pf?.name ?? `Portfolio ${tx.portfolioId}`;
              const date      = tx.executedAt ? new Date(tx.executedAt).toLocaleString() : '—';
              return (
                <Fragment key={tx.transactionId ?? i}>
                  <div className="gc">
                    <span className={`tx-key ${isDeposit ? 'b' : 's'}`}>{tx.type}</span>
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-2)' }}>{pfName}</div>
                  <div className="gc r num" style={{ color: isDeposit ? 'var(--pos)' : 'var(--neg)' }}>
                    {isDeposit ? '+' : '−'}${(Number(tx.amount) || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{date}</div>
                </Fragment>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
