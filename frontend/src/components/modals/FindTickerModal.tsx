import { useState } from 'react';
import type { FormEvent } from 'react';
import { XIcon, SearchIcon, PlusIcon } from '../shared/Icons';
import { api } from '../../api';
import type { CompanyProfile } from '../../types';

interface FindTickerModalProps {
  onClose: () => void;
}

export default function FindTickerModal({ onClose }: FindTickerModalProps) {
  const [query, setQuery]     = useState('');
  const [profile, setProfile] = useState<CompanyProfile | null>(null);
  const [tracked, setTracked] = useState(false);
  const [loading, setLoading]   = useState(false);
  const [tracking, setTracking] = useState(false);
  const [error, setError]       = useState('');

  async function handleSearch(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!query.trim()) return;
    setError('');
    setLoading(true);
    try {
      const res = await api.market.getProfile(query.trim().toUpperCase());
      setProfile(res);
      setTracked(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed');
      setProfile(null);
    } finally {
      setLoading(false);
    }
  }

  async function handleTrack() {
    if (!profile) return;
    setTracking(true);
    try {
      await api.market.addTracking(profile.ticker);
      setTracked(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to track ticker');
    } finally {
      setTracking(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-card">
        <div className="modal-header">
          <h3>Find ticker</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8 }}>
          <div className="modal-field" style={{ flex: 1, margin: 0 }}>
            <input
              value={query}
              onChange={e => setQuery(e.target.value.toUpperCase())}
              placeholder="AAPL, TSLA, MSFT…"
              autoFocus
            />
          </div>
          <button type="submit" className="btn" disabled={loading}>
            <SearchIcon size={14} /> {loading ? '…' : 'Search'}
          </button>
        </form>

        {error && <div className="login-error" style={{ marginTop: 12 }}>{error}</div>}

        {profile && (
          <div style={{ marginTop: 20, padding: '16px', border: '1px solid var(--rule)', borderRadius: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 6 }}>
              <span className="sym">{profile.ticker}</span>
              <button className="btn" onClick={() => { void handleTrack(); }} disabled={tracked || tracking}>
                {tracking ? '…' : tracked ? '✓ Tracked' : <><PlusIcon size={13} /> Track</>}
              </button>
            </div>
            <div style={{ fontSize: '13px', color: 'var(--ink-2)', marginBottom: 4 }}>{profile.name}</div>
            <div style={{ fontSize: '12px', color: 'var(--ink-3)', fontFamily: 'var(--mono)' }}>
              {profile.exchange} · {profile.country} · {profile.currency}
            </div>
          </div>
        )}

        <div className="modal-actions" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
