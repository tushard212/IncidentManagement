import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WebSocketMessage } from '../types';

class WebSocketService {
  private client: Client | null = null;
  private subscribers: ((msg: WebSocketMessage) => void)[] = [];

  connect() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected');
        this.client?.subscribe('/topic/incidents', (message) => {
          const payload: WebSocketMessage = JSON.parse(message.body);
          this.subscribers.forEach((cb) => cb(payload));
        });
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
      },
    });
    this.client.activate();
  }

  subscribe(callback: (msg: WebSocketMessage) => void) {
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter((cb) => cb !== callback);
    };
  }

  disconnect() {
    this.client?.deactivate();
  }
}

export const wsService = new WebSocketService();
