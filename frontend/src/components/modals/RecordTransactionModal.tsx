import { useState } from 'react';
import type { FormEvent } from 'react';
import { XIcon } from '../shared/Icons';
import type { TransactionType } from '../../types';

interface RecordTransactionModalProps {
  onClose: () => void;
  onSuccess: (data: { transactionType: TransactionType; ticker: string; quantity: number }) => Promise<void>;
}

export default function RecordTransactionModal({ onClose, onSuccess }: RecordTransactionModalProps) {
  const [type, setType]       = useState<TransactionType>('BUY');
  const [ticker, setTicker]   = useState('');
  const [quantity, setQty]    = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const [errors, setErrors]   = useState<{ [k: string]: string }>({});

  function clearFieldError(field: string) {
    if (!errors[field]) return;
    setErrors(prev => { const n = { ...prev }; delete n[field]; return n; });
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const errs: { [k: string]: string } = {};
    if (!ticker.trim()) errs['ticker'] = 'Ticker is required';
    const qty = parseFloat(quantity);
    if (!quantity.trim() || !Number.isFinite(qty) || qty <= 0) errs['quantity'] = 'Must be greater than 0';
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    setErrors({});
    setError('');
    setLoading(true);
    try {
      await onSuccess({ transactionType: type, ticker: ticker.trim().toUpperCase(), quantity: qty });
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
          <h3>Record transaction</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label>Type</label>
            <select value={type} onChange={e => setType(e.target.value as TransactionType)}>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </select>
          </div>
          <div className="modal-field">
            <label>Ticker</label>
            <input
              type="text"
              value={ticker}
              onChange={e => { setTicker(e.target.value); clearFieldError('ticker'); }}
              placeholder="e.g. AAPL"
              className={errors['ticker'] ? 'invalid' : undefined}
            />
            {errors['ticker'] && <span className="field-error">{errors['ticker']}</span>}
          </div>
          <div className="modal-field">
            <label>Quantity</label>
            <input
              type="number"
              step="0.0001"
              value={quantity}
              onChange={e => { setQty(e.target.value); clearFieldError('quantity'); }}
              placeholder="0"
              className={errors['quantity'] ? 'invalid' : undefined}
            />
            {errors['quantity'] && <span className="field-error">{errors['quantity']}</span>}
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary" disabled={loading}>
              {loading ? 'Saving…' : 'Record'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
