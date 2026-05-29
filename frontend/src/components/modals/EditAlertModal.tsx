import { useState } from 'react';
import type { FormEvent } from 'react';
import { XIcon } from '../shared/Icons';
import { api } from '../../api';
import type { NormalizedAlert } from '../../types';

interface EditAlertModalProps {
  alert: NormalizedAlert;
  onClose: () => void;
  onUpdated: (updated: NormalizedAlert) => void;
}

export default function EditAlertModal({ alert, onClose, onUpdated }: EditAlertModalProps) {
  const [targetPrice, setTargetPrice] = useState(String(alert.targetPrice ?? ''));
  const [saving, setSaving]           = useState(false);
  const [error, setError]             = useState('');

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const parsed = parseFloat(targetPrice);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      setError('Enter a valid price greater than 0');
      return;
    }
    setError('');
    setSaving(true);
    try {
      const updated = await api.alerts.update(alert.id, { targetPrice: parsed });
      onUpdated({ ...alert, targetPrice: updated.targetPrice ?? parsed });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update alert');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-card">
        <div className="modal-header">
          <h3>Edit alert · {alert.ticker}</h3>
          <button className="modal-close" onClick={onClose}><XIcon size={16} /></button>
        </div>

        {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label>Target price</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={targetPrice}
              onChange={e => setTargetPrice(e.target.value)}
              required
            />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
