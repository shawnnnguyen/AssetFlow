import { useState, useEffect } from 'react';
import { api } from '../api';

const TABS = ['Trades', 'Cash Flow'];

export default function GlobalTransactionsView({ trackedStocksRef }) {
  const [tab, setTab]               = useState('Trades');
  const [trades, setTrades]         = useState([]);
  const [cashTxs, setCashTxs]       = useState([]);
  const [loadingTrades, setLoadingTrades]   = useState(true);
  const [loadingCash, setLoadingCash]       = useState(false);
  const [cashFetched, setCashFetched]       = useState(false);

  useEffect(() => {
    setLoadingTrades(true);
    api.transactions.getAll(50)
      .then(res => setTrades(res.content ?? []))
      .catch(console.error)
      .finally(() => setLoadingTrades(false));
  }, []);

  useEffect(() => {
    if (tab !== 'Cash Flow' || cashFetched) return;
    setLoadingCash(true);
    api.cashTransactions.getAllGlobal(50)
      .then(res => {
        setCashTxs(res.content ?? []);
        setCashFetched(true);
      })
      .catch(console.error)
      .finally(() => setLoadingCash(false));
  }, [tab, cashFetched]);

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
        <div className="filter">
          {TABS.map(t => (
            <button key={t} className={`chip${tab === t ? ' on' : ''}`} onClick={() => setTab(t)}>{t}</button>
          ))}
        </div>
      </div>

      {tab === 'Trades' && (
        <>
          <div className="tx-full-grid">
            <div className="gh">Type</div>
            <div className="gh">Ticker</div>
            <div className="gh r">Qty</div>
            <div className="gh r">Price</div>
            <div className="gh r">Value</div>
            <div className="gh">Date</div>
          </div>
          {loadingTrades ? (
            <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>Loading…</div>
          ) : trades.length === 0 ? (
            <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>No trades yet.</div>
          ) : (
            trades.map((tx, i) => {
              const isBuy  = tx.type === 'BUY';
              const stocks = trackedStocksRef?.current ?? {};
              const ticker = Object.values(stocks).find(s => s.assetId === tx.assetId)?.ticker ?? '—';
              const value  = (Number(tx.quantity) || 0) * (Number(tx.pricePerUnit) || 0);
              const date   = tx.executedAt ? new Date(tx.executedAt).toLocaleDateString() : '—';
              return (
                <div key={tx.transactionId ?? i} className="tx-full-grid" style={{ display: 'contents' }}>
                  <div className="gc"><span className={`tx-key ${isBuy ? 'b' : 's'}`}>{tx.type}</span></div>
                  <div className="gc"><span className="sym" style={{ fontWeight: 500 }}>{ticker}</span></div>
                  <div className="gc r num">{tx.quantity}</div>
                  <div className="gc r num">
                    ${(Number(tx.pricePerUnit) || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className="gc r num">
                    ${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{date}</div>
                </div>
              );
            })
          )}
        </>
      )}

      {tab === 'Cash Flow' && (
        <>
          <div className="tx-full-grid" style={{ gridTemplateColumns: '1fr 2fr 1fr 1fr' }}>
            <div className="gh">Type</div>
            <div className="gh">Portfolio</div>
            <div className="gh r">Amount</div>
            <div className="gh">Date</div>
          </div>
          {loadingCash ? (
            <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>Loading…</div>
          ) : cashTxs.length === 0 ? (
            <div style={{ padding: '24px 20px', color: 'var(--ink-3)', fontSize: '13px' }}>No cash movements yet.</div>
          ) : (
            cashTxs.map((tx, i) => {
              const isDeposit = tx.type === 'DEPOSIT';
              const date      = tx.executedAt ? new Date(tx.executedAt).toLocaleDateString() : '—';
              return (
                <div key={tx.transactionId ?? i} className="tx-full-grid" style={{ display: 'contents', gridTemplateColumns: '1fr 2fr 1fr 1fr' }}>
                  <div className="gc">
                    <span className={`tx-key ${isDeposit ? 'b' : 's'}`}>{tx.type}</span>
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-2)' }}>
                    Portfolio {tx.portfolioId}
                  </div>
                  <div className="gc r num" style={{ color: isDeposit ? 'var(--pos)' : 'var(--neg)' }}>
                    {isDeposit ? '+' : '−'}${(Number(tx.amount) || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className="gc" style={{ fontSize: 12, color: 'var(--ink-3)' }}>{date}</div>
                </div>
              );
            })
          )}
        </>
      )}
    </div>
  );
}
