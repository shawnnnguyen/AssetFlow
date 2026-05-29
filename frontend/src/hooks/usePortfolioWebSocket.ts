import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { parseMessage } from '../lib/parseMessage';
import type { PortfolioPerf } from '../types';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

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

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        subRef.current = client.subscribe(
          `/user/queue/portfolio/${portfolioId}`,
          msg => setPortfolioPerf(parseMessage<PortfolioPerf>(msg)),
        );
      },
    });

    client.activate();

    return () => {
      subRef.current?.unsubscribe();
      void client.deactivate();
    };
  }, [token, portfolioId]);

  return { portfolioPerf };
}
