import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { XIcon } from '../shared/Icons';
import { api } from '../../api';
import type { TrackedStock, PriceAlert } from '../../types';

interface NewPriceAlertModalProps {
  trackedStocks?: TrackedStock[];
  onClose: () => void;
  onCreated?: ((alert: PriceAlert) => void) | undefined;
}

export default function NewPriceAlertModal({ trackedStocks = [], onClose, onCreated }: NewPriceAlertModalProps) {
  const tickers = trackedStocks.map(s => s.ticker).sort();

  const [ticker, setTicker]           = useState<string>(tickers[0] ?? '');
  const [targetPrice, setTargetPrice] = useState('');
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');

  useEffect(() => {
    const first = trackedStocks[0]?.ticker;
    if (first !== undefined && (ticker === '' || !tickers.includes(ticker))) {
      setTicker(first);
    }
  }, [trackedStocks]);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const alert = await api.alerts.create({ ticker, targetPrice: parseFloat(targetPrice) });
      onCreated?.(alert);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create alert');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-card">
        <div className="modal-header">
          <h3>New price alert</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

        {tickers.length === 0 ? (
          <div style={{ color: 'var(--ink-3)', fontSize: 13 }}>
            No tracked tickers yet. Use Find Ticker to add stocks to track first.
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="modal-field">
              <label>Ticker</label>
              <select value={ticker} onChange={e => setTicker(e.target.value)} required>
                {tickers.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="modal-field">
              <label>Target price (USD)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={targetPrice}
                onChange={e => setTargetPrice(e.target.value)}
                placeholder="0.00"
                required
              />
            </div>
            <div className="modal-actions">
              <button type="button" className="btn" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn primary" disabled={loading}>
                {loading ? 'Creating…' : 'Create alert'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
