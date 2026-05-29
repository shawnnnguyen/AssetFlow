import type { DisplayTransaction } from '../../types';

interface RecentTransactionsProps {
  transactions?: DisplayTransaction[];
}

export default function RecentTransactions({ transactions = [] }: RecentTransactionsProps) {
  return (
    <div className="rail-section" style={{ paddingBottom: 22 }}>
      <div className="rail-lbl">Recent transactions</div>
      {transactions.length === 0 ? (
        <div style={{ fontSize: '12.5px', color: 'var(--ink-3)' }}>No transactions yet.</div>
      ) : (
        transactions.map((tx, i) => {
          const isBuy = tx.transactionType === 'BUY';
          return (
            <div key={tx.id ?? i} className="tx-row">
              <div>
                <span className={`tx-key ${isBuy ? 'b' : 's'}`}>{tx.transactionType}</span>
                {' '}{tx.ticker} × {tx.quantity}
              </div>
              <div className="num" style={{ color: 'var(--ink-2)', fontSize: 12 }}>
                ${(tx.price * tx.quantity).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}
