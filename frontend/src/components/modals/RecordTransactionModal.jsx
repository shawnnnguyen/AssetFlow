import { useState } from 'react';
import { XIcon } from '../shared/Icons';

export default function RecordTransactionModal({ onClose, onSuccess }) {
  const [type, setType]       = useState('BUY');
  const [ticker, setTicker]   = useState('');
  const [qty, setQty]         = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onSuccess({ transactionType: type, ticker, quantity: parseFloat(qty) });
      onClose();
    } catch (err) {
      setError(err.message);
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
            <select value={type} onChange={e => setType(e.target.value)}>
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </div>
          <div className="modal-field">
            <label>Ticker</label>
            <input value={ticker} onChange={e => setTicker(e.target.value.toUpperCase())} placeholder="e.g. AAPL" required />
          </div>
          <div className="modal-field">
            <label>Quantity</label>
            <input type="number" step="0.0001" min="0" value={qty} onChange={e => setQty(e.target.value)} placeholder="0" required />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary" disabled={loading}>
              {loading ? 'Saving…' : `Record ${type}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
