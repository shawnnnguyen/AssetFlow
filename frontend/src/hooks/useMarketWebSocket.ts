import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { parseMessage } from '../lib/parseMessage';
import type { MarketUpdate } from '../types';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

interface UseMarketWebSocketReturn {
  lastPrice: MarketUpdate | null;
  connected: boolean;
}

export function useMarketWebSocket(token: string | null): UseMarketWebSocketReturn {
  const [lastPrice, setLastPrice] = useState<MarketUpdate | null>(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/prices', msg => {
          setLastPrice(parseMessage<MarketUpdate>(msg));
        });
      },
      onDisconnect: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
    return () => { void client.deactivate(); };
  }, [token]);

  return { lastPrice, connected };
}
