import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import type { ScaleRealtimeData, WebSocketContextType } from '../types';
import SockJS from 'sockjs-client';
// ...existing code...
import { toast } from 'react-toastify';

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/api/v1/ws-scalehub';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({ children }) => {
  const [client, setClient] = useState<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [scaleData, setScaleData] = useState<Map<number, ScaleRealtimeData>>(new Map());
  const [subscriptions, setSubscriptions] = useState<Map<string, StompSubscription>>(new Map());

  // Initialize WebSocket connection
  useEffect(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      onConnect: () => {
        console.log('WebSocket connected');
        setConnected(true);
        toast.success('Real-time connection established');
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        setConnected(false);
        toast.warning('Real-time connection lost');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        toast.error('WebSocket connection error');
      },
    });

    stompClient.activate();
    setClient(stompClient);

    return () => {
      if (stompClient) {
        stompClient.deactivate();
      }
    };
  }, []);

  // Subscribe to all scales updates
  const subscribeAll = useCallback(() => {
    if (!client || !connected) return;

    const destination = '/topic/scales';
    
    // Check if already subscribed
    if (subscriptions.has(destination)) {
      return;
    }

    const subscription = client.subscribe(destination, (message) => {
      try {
        const data: ScaleRealtimeData = JSON.parse(message.body);
        console.log('[WebSocket] Received scale data:', data);
        setScaleData((prev) => {
          const updated = new Map(prev);
          updated.set(data.scaleId, data);
          return updated;
        });
      } catch (error) {
        console.error('Error parsing scale data:', error, 'Body:', message.body);
      }
    });

    setSubscriptions((prev) => {
      const updated = new Map(prev);
      updated.set(destination, subscription);
      return updated;
    });

    console.log('Subscribed to all scales');
  }, [client, connected, subscriptions]);

  // Subscribe to specific scale updates
  const subscribe = useCallback(
    (scaleId: number) => {
      if (!client || !connected) return;

      const destination = `/topic/scale/${scaleId}`;
      
      // Check if already subscribed
      if (subscriptions.has(destination)) {
        return;
      }

      const subscription = client.subscribe(destination, (message) => {
        try {
          const data: ScaleRealtimeData = JSON.parse(message.body);
          console.log(`[WebSocket] Received data for scale ${data.scaleId}:`, data);
          setScaleData((prev) => {
            const updated = new Map(prev);
            updated.set(data.scaleId, data);
            return updated;
          });
        } catch (error) {
          console.error(`Error parsing scale ${scaleId} data:`, error, 'Body:', message.body);
        }
      });

      setSubscriptions((prev) => {
        const updated = new Map(prev);
        updated.set(destination, subscription);
        return updated;
      });

      console.log(`Subscribed to scale ${scaleId}`);
    },
    [client, connected, subscriptions]
  );

  // Unsubscribe from specific scale
  const unsubscribe = useCallback(
    (scaleId: number) => {
      const destination = `/topic/scale/${scaleId}`;
      const subscription = subscriptions.get(destination);

      if (subscription) {
        subscription.unsubscribe();
        setSubscriptions((prev) => {
          const updated = new Map(prev);
          updated.delete(destination);
          return updated;
        });
        console.log(`Unsubscribed from scale ${scaleId}`);
      }
    },
    [subscriptions]
  );

  const value: WebSocketContextType = {
    connected,
    scaleData,
    subscribe,
    unsubscribe,
    subscribeAll,
  };

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
};

export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};
