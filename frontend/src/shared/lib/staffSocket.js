import { Client } from '@stomp/stompjs';
import { API_BASE, getToken, refreshAccessToken } from './api.js';

const WS_BASE = import.meta.env.VITE_WS_URL || API_BASE;

function resolveBrokerUrl() {
  if (WS_BASE) {
    const base = WS_BASE.replace(/\/+$/, '');
    const wsBase = base.replace(/^http/i, 'ws');
    return `${wsBase}/ws`;
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws`;
}

function isStaffBroadcastRole(role) {
  return role === 'ADMIN' || role === 'STAFF' || role === 'RECEPTIONIST';
}

function isAuthErrorText(text) {
  if (!text) return false;
  const lower = String(text).toLowerCase();
  return lower.includes('token') || lower.includes('hạn') || lower.includes('expired') || lower.includes('unauthorized');
}

export function createStaffSocket({ role, onNotification, onStateChange, onConnected }) {
  let authRetryCount = 0;
  let refreshing = false;

  const client = new Client({
    brokerURL: resolveBrokerUrl(),
    connectHeaders: {
      Authorization: `Bearer ${getToken()}`,
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    beforeConnect: () => {
      client.connectHeaders = {
        Authorization: `Bearer ${getToken()}`,
      };
    },
    onConnect: () => {
      authRetryCount = 0;
      onStateChange?.(true);

      const handleFrame = (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          onNotification?.(payload);
        } catch {
          /* bỏ qua payload không hợp lệ */
        }
      };

      if (isStaffBroadcastRole(role)) {
        client.subscribe('/topic/staff/appointments', handleFrame);
      }
      if (role === 'DOCTOR') {
        client.subscribe('/user/queue/appointments', handleFrame);
      }

      onConnected?.();
    },
    onDisconnect: () => {
      onStateChange?.(false);
    },
    onWebSocketError: () => {
      onStateChange?.(false);
    },
    onStompError: async (frame) => {
      onStateChange?.(false);
      const errMsg = frame?.headers?.message || frame?.body || '';
      if (isAuthErrorText(errMsg)) {
        await retryWithFreshToken();
      }
    },
    onWebSocketClose: async (event) => {
      onStateChange?.(false);
      if (event?.code === 1008 || isAuthErrorText(event?.reason)) {
        await retryWithFreshToken();
      }
    },
  });

  async function retryWithFreshToken() {
    if (refreshing || authRetryCount >= 2) {
      return;
    }
    refreshing = true;
    try {
      authRetryCount += 1;
      const ok = await refreshAccessToken();
      if (ok && getToken()) {
        client.connectHeaders = {
          Authorization: `Bearer ${getToken()}`,
        };
        await client.deactivate();
        client.activate();
      }
    } finally {
      refreshing = false;
    }
  }

  client.activate();
  return client;
}
