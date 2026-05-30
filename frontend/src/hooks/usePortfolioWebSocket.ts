import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { parseMessage } from '../lib/parseMessage';
import type { PortfolioPerf } from '../types';
import { BACKEND_URL } from '../config';

interface UsePortfolioWebSocketReturn {
  portfolioPerf: PortfolioPerf | null;
}

export function usePortfolioWebSocket(
  portfolioId: number | null,
  token: string | null,
): UsePortfolioWebSocketReturn {
  const [portfolioPerf, setPortfolioPerf] = useState<PortfolioPerf | null>(null);
  const subRef = useRef<StompSubscription | null>(null);

  useEffect(() => {
    if (!token || !portfolioId) return;

    let cancelled = false;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (cancelled) return;
        subRef.current = client.subscribe(
          `/user/queue/portfolio/${portfolioId}`,
          msg => setPortfolioPerf(parseMessage<PortfolioPerf>(msg)),
        );
      },
    });

    client.activate();

    return () => {
      cancelled = true;
      subRef.current?.unsubscribe();
      subRef.current = null;
      void client.deactivate();
    };
  }, [token, portfolioId]);

  return { portfolioPerf };
}
