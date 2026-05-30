import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { parseMessage } from '../lib/parseMessage';
import type { TriggeredAlert } from '../types';
import { BACKEND_URL } from '../config';

interface UseAlertWebSocketReturn {
  triggeredAlerts: TriggeredAlert[];
}

export function useAlertWebSocket(userId: string | null, token: string | null): UseAlertWebSocketReturn {
  const [triggeredAlerts, setTriggeredAlerts] = useState<TriggeredAlert[]>([]);
  const clientRef = useRef<Client | null>(null);
  const subRef    = useRef<StompSubscription | null>(null);

  useEffect(() => {
    if (!userId || !token) return;

    let cancelled = false;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws-alerts`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (cancelled) return;
        subRef.current = client.subscribe(`/topic/alerts/${userId}`, msg => {
          const alert = parseMessage<TriggeredAlert>(msg);
          setTriggeredAlerts(prev => [alert, ...prev].slice(0, 5));
        });
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      cancelled = true;
      subRef.current?.unsubscribe();
      subRef.current = null;
      void client.deactivate();
    };
  }, [userId, token]);

  return { triggeredAlerts };
}
