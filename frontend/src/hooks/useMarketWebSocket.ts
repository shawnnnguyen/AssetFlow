import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { parseMessage } from '../lib/parseMessage';
import type { MarketUpdate } from '../types';
import { BACKEND_URL } from '../config';
import { useToast } from '../context/ToastContext';

interface UseMarketWebSocketReturn {
  lastPrice: MarketUpdate | null;
  connected: boolean;
}

export function useMarketWebSocket(token: string | null): UseMarketWebSocketReturn {
  const [lastPrice, setLastPrice] = useState<MarketUpdate | null>(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subRef    = useRef<StompSubscription | null>(null);
  const { addToast } = useToast();

  useEffect(() => {
    let cancelled         = false;
    let everConnected     = false;
    let toastedDisconnect = false;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        if (cancelled) return;
        if (toastedDisconnect) { addToast('Live prices reconnected', 'info'); toastedDisconnect = false; }
        everConnected = true;
        setConnected(true);
        subRef.current = client.subscribe('/topic/prices', msg => {
          setLastPrice(parseMessage<MarketUpdate>(msg));
        });
      },
      onWebSocketClose: () => {
        if (cancelled) return;
        if (everConnected && !toastedDisconnect) {
          addToast('Live prices disconnected — reconnecting…', 'warn');
          toastedDisconnect = true;
        }
        setConnected(false);
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      cancelled = true;
      setConnected(false);
      subRef.current?.unsubscribe();
      subRef.current = null;
      void client.deactivate();
    };
  }, [token, addToast]);

  return { lastPrice, connected };
}
