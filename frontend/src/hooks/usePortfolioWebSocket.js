import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

export function usePortfolioWebSocket(portfolioId, token) {
  const [portfolioPerf, setPortfolioPerf] = useState(null);
  const subRef = useRef(null);

  useEffect(() => {
    if (!token || !portfolioId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        subRef.current = client.subscribe(
          `/user/queue/portfolio/${portfolioId}`,
          (msg) => setPortfolioPerf(JSON.parse(msg.body))
        );
      },
    });

    client.activate();

    return () => {
      subRef.current?.unsubscribe();
      client.deactivate();
    };
  }, [token, portfolioId]);

  return { portfolioPerf };
}
