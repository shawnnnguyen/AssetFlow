import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import { useToast } from '../context/ToastContext';
import type { Portfolio } from '../types';

export function usePortfolios(userId: string | null) {
  const { addToast } = useToast();
  const [portfolios, setPortfolios]                 = useState<Portfolio[]>([]);
  const [currentPortfolioId, setCurrentPortfolioId] = useState<number | null>(null);
  const currentPortfolioIdRef = useRef<number | null>(null);

  // Synchronously keeps ref in sync so async callbacks detect a portfolio switch
  // without a one-render lag (an async useEffect would miss the window between
  // setState and the next commit).
  function setPortfolioId(id: number | null) {
    currentPortfolioIdRef.current = id;
    setCurrentPortfolioId(id);
  }

  useEffect(() => {
    if (!userId) return;
    setPortfolios([]);
    setPortfolioId(null);
    let stale = false;
    void api.portfolios.getAll()
      .then(pfs => {
        if (stale) return;
        setPortfolios(pfs);
        const first = pfs[0];
        if (first !== undefined) setPortfolioId(first.id);
      })
      .catch(e => { console.error(e); addToast('Could not load portfolios'); });
    return () => { stale = true; };
  }, [userId]);

  const currentPortfolio = portfolios.find(p => p.id === currentPortfolioId);

  async function handleSelectPortfolio(id: number) {
    setPortfolioId(id);
    try {
      const fresh = await api.portfolios.getById(id);
      setPortfolios(prev => prev.map(p => p.id === fresh.id ? { ...p, ...fresh } : p));
    } catch (e) {
      console.error(e);
      addToast('Could not refresh portfolio');
    }
  }

  return {
    portfolios,
    setPortfolios,
    currentPortfolioId,
    setCurrentPortfolioId: setPortfolioId,
    currentPortfolio,
    currentPortfolioIdRef,
    handleSelectPortfolio,
  };
}
