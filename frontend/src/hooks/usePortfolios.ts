import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import type { Portfolio } from '../types';

export function usePortfolios(userId: string | null) {
  const [portfolios, setPortfolios]                 = useState<Portfolio[]>([]);
  const [currentPortfolioId, setCurrentPortfolioId] = useState<number | null>(null);
  const currentPortfolioIdRef = useRef<number | null>(null);

  // Keep ref in sync so async callbacks can detect a portfolio switch.
  useEffect(() => {
    currentPortfolioIdRef.current = currentPortfolioId;
  }, [currentPortfolioId]);

  useEffect(() => {
    if (!userId) return;
    setPortfolios([]);
    setCurrentPortfolioId(null);
    let stale = false;
    void api.portfolios.getAll()
      .then(pfs => {
        if (stale) return;
        setPortfolios(pfs);
        const first = pfs[0];
        if (first !== undefined) setCurrentPortfolioId(first.id);
      })
      .catch(console.error);
    return () => { stale = true; };
  }, [userId]);

  const currentPortfolio = portfolios.find(p => p.id === currentPortfolioId);

  async function handleSelectPortfolio(id: number) {
    setCurrentPortfolioId(id);
    try {
      const fresh = await api.portfolios.getById(id);
      setPortfolios(prev => prev.map(p => p.id === fresh.id ? { ...p, ...fresh } : p));
    } catch (e) {
      console.error(e);
    }
  }

  return {
    portfolios,
    setPortfolios,
    currentPortfolioId,
    setCurrentPortfolioId,
    currentPortfolio,
    currentPortfolioIdRef,
    handleSelectPortfolio,
  };
}
