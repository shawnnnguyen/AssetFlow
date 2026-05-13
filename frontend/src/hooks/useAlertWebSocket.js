import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

export function useAlertWebSocket(userId, token) {
  const [triggeredAlerts, setTriggeredAlerts] = useState([]);
  const clientRef = useRef(null);
  const subRef    = useRef(null);

  useEffect(() => {
    if (!userId || !token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-alerts`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        subRef.current = client.subscribe(`/topic/alerts/${userId}`, (msg) => {
          const alert = JSON.parse(msg.body);
          setTriggeredAlerts(prev => [alert, ...prev].slice(0, 5));
        });
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      subRef.current?.unsubscribe();
      client.deactivate();
    };
  }, [userId, token]);

  return { triggeredAlerts };
}
