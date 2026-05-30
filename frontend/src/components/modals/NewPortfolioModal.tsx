import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { XIcon } from '../shared/Icons';
import { api } from '../../api';
import type { Currency } from '../../types';

const FALLBACK_CURRENCIES: Currency[] = [
  { code: 'USD', symbol: '$' },
  { code: 'EUR', symbol: '€' },
  { code: 'GBP', symbol: '£' },
  { code: 'JPY', symbol: '¥' },
  { code: 'CAD', symbol: 'C$' },
];

interface NewPortfolioModalProps {
  onClose: () => void;
  onCreate: (body: { name: string; currencyCode: string }) => Promise<void>;
}

export default function NewPortfolioModal({ onClose, onCreate }: NewPortfolioModalProps) {
  const [name, setName]             = useState('');
  const [currencyCode, setCurrency] = useState('USD');
  const [currencies, setCurrencies] = useState<Currency[]>(FALLBACK_CURRENCIES);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState('');
  const [nameError, setNameError]   = useState('');

  useEffect(() => {
    void api.currencies.getAll()
      .then(list => {
        if (list && list.length > 0) {
          setCurrencies(list);
          if (!list.find(c => c.code === 'USD')) {
            setCurrency(list[0]?.code ?? 'USD');
          }
        }
      })
      .catch(() => {
        // fallback list already in state — no action needed
      });
  }, []);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!name.trim()) { setNameError('Portfolio name is required'); return; }
    setNameError('');
    setError('');
    setLoading(true);
    try {
      await onCreate({ name: name.trim(), currencyCode });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create portfolio');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-card" style={{ minWidth: 320 }}>
        <div className="modal-header">
          <h3>New portfolio</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label>Portfolio name</label>
            <input
              value={name}
              onChange={e => { setName(e.target.value); if (nameError) setNameError(''); }}
              placeholder="e.g. Long-term"
              autoFocus
              className={nameError ? 'invalid' : undefined}
            />
            {nameError && <span className="field-error">{nameError}</span>}
          </div>
          <div className="modal-field">
            <label>Currency</label>
            <select value={currencyCode} onChange={e => setCurrency(e.target.value)}>
              {currencies.map(c => (
                <option key={c.code} value={c.code}>{c.code} {c.symbol}</option>
              ))}
            </select>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary" disabled={loading}>
              {loading ? 'Creating…' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
