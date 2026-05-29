import { useState } from 'react';
import type { FormEvent } from 'react';
import { XIcon } from '../shared/Icons';
import { api } from '../../api';
import type { CashTransactionType } from '../../types';

interface CashTransactionModalProps {
  portfolioId: number;
  onClose: () => void;
  onSuccess?: (() => void) | undefined;
}

export default function CashTransactionModal({ portfolioId, onClose, onSuccess }: CashTransactionModalProps) {
  const [type, setType]       = useState<CashTransactionType>('DEPOSIT');
  const [amount, setAmount]   = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.cashTransactions.create(portfolioId, {
        portfolioId,
        type,
        amount: parseFloat(amount),
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Transaction failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-card">
        <div className="modal-header">
          <h3>Deposit / Withdraw</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label>Type</label>
            <select value={type} onChange={e => setType(e.target.value as CashTransactionType)}>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAWAL">Withdrawal</option>
            </select>
          </div>
          <div className="modal-field">
            <label>Amount</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              placeholder="0.00"
              required
            />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary" disabled={loading}>
              {loading ? 'Saving…' : type === 'DEPOSIT' ? 'Deposit' : 'Withdraw'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
