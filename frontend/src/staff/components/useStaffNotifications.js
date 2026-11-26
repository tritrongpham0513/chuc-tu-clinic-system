import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../../shared/lib/api.js';
import { createStaffSocket } from '../../shared/lib/staffSocket.js';

export function useStaffNotifications(user) {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [connected, setConnected] = useState(false);

  const itemsRef = useRef([]);
  const unreadRef = useRef(0);
  const mountedRef = useRef(true);

  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  useEffect(() => {
    unreadRef.current = unread;
  }, [unread]);

  const fetchNotifications = useCallback(async ({ silent = false } = {}) => {
    if (!user?.id) return;
    if (!silent) setLoading(true);
    try {
      const [listData, countData] = await Promise.all([
        api.get('/staff/notifications?page=1&limit=20'),
        api.get('/staff/notifications/unread-count'),
      ]);
      if (!mountedRef.current) return;
      const nextItems = Array.isArray(listData?.items) ? listData.items : [];
      const nextUnread = typeof countData?.unread === 'number'
        ? countData.unread
        : typeof listData?.unread === 'number'
        ? listData.unread
        : 0;
      setItems(nextItems);
      setUnread(nextUnread);
      if (!silent) setError('');
    } catch (err) {
      if (!mountedRef.current) return;
      if (!silent) {
        setError(err?.message || 'Không tải được danh sách thông báo.');
      }
    } finally {
      if (mountedRef.current && !silent) {
        setLoading(false);
      }
    }
  }, [user?.id]);

  const fetchUnreadOnly = useCallback(async () => {
    if (!user?.id) return;
    try {
      const countData = await api.get('/staff/notifications/unread-count');
      if (!mountedRef.current) return;
      if (typeof countData?.unread === 'number') {
        setUnread(countData.unread);
      }
    } catch {
      /* giữ nguyên trạng thái hiện tại khi mất mạng */
    }
  }, [user?.id]);

  const handleIncomingNotification = useCallback((dto) => {
    if (!dto || !mountedRef.current) return;
    setItems((prev) => {
      const exists = prev.some(
        (item) =>
          (dto.id != null && item.id === dto.id) ||
          (dto.appointmentId != null && item.appointmentId === dto.appointmentId && item.type === dto.type && !item.read)
      );
      if (exists) {
        return prev;
      }
      setUnread((u) => u + 1);
      return [dto, ...prev].slice(0, 20);
    });
    // Đồng bộ lại ID hộp thư riêng của user hiện tại (quan trọng khi nhiều tiếp tân dùng chung topic)
    fetchNotifications({ silent: true });
  }, [fetchNotifications]);

  // Nạp ban đầu và kết nối WebSocket
  useEffect(() => {
    mountedRef.current = true;
    if (!user?.id) {
      setItems([]);
      setUnread(0);
      setConnected(false);
      return undefined;
    }

    fetchNotifications();

    const client = createStaffSocket({
      role: user.role,
      onNotification: handleIncomingNotification,
      onStateChange: (isUp) => {
        if (mountedRef.current) {
          setConnected(isUp);
        }
      },
      onConnected: () => {
        fetchNotifications({ silent: true });
      },
    });

    return () => {
      mountedRef.current = false;
      client.deactivate();
    };
  }, [user?.id, user?.role, fetchNotifications, handleIncomingNotification]);

  // Fallback polling: khi connected === false quá 10 giây, poll unread-count mỗi 60 giây
  useEffect(() => {
    if (!user?.id || connected) return undefined;

    let pollInterval = null;
    const delayTimer = setTimeout(() => {
      fetchUnreadOnly();
      pollInterval = setInterval(fetchUnreadOnly, 60_000);
    }, 10_000);

    return () => {
      clearTimeout(delayTimer);
      if (pollInterval) clearInterval(pollInterval);
    };
  }, [user?.id, connected, fetchUnreadOnly]);

  const markRead = useCallback(async (id) => {
    if (!id) return;
    const prevItems = itemsRef.current;
    const prevUnread = unreadRef.current;
    const target = prevItems.find((it) => it.id === id);
    const wasUnread = Boolean(target && !target.read);

    setItems((curr) => curr.map((it) => (it.id === id ? { ...it, read: true } : it)));
    if (wasUnread) {
      setUnread((u) => Math.max(0, u - 1));
    }

    try {
      await api.post(`/staff/notifications/${id}/read`);
      setError('');
    } catch (err) {
      if (!mountedRef.current) return;
      setItems(prevItems);
      setUnread(prevUnread);
      setError(err?.message || 'Không đánh dấu đã đọc được thông báo.');
    }
  }, []);

  const markAllRead = useCallback(async () => {
    const prevItems = itemsRef.current;
    const prevUnread = unreadRef.current;

    setItems((curr) => curr.map((it) => ({ ...it, read: true })));
    setUnread(0);

    try {
      await api.post('/staff/notifications/read-all');
      setError('');
    } catch (err) {
      if (!mountedRef.current) return;
      setItems(prevItems);
      setUnread(prevUnread);
      setError(err?.message || 'Không đánh dấu tất cả đã đọc được.');
    }
  }, []);

  return {
    items,
    unread,
    loading,
    error,
    connected,
    reload: () => fetchNotifications(),
    markRead,
    markAllRead,
  };
}
