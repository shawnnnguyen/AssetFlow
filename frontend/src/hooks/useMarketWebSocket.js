import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

export function useMarketWebSocket(token) {
  const [lastPrice, setLastPrice] = useState(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-market`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/prices', (msg) => {
          setLastPrice(JSON.parse(msg.body));
        });
      },
      onDisconnect: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [token]);

  return { lastPrice, connected };
}
