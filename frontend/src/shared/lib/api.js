// API Client: tập trung mọi lời gọi REST tới Backend.
// Địa chỉ API (VITE_API_URL trong .env); mặc định gọi /api/v1 cùng địa chỉ
export const API_BASE = import.meta.env.VITE_API_URL || '';

export const getToken = () => {
  try {
    return localStorage.getItem('ct_token') || '';
  } catch {
    return '';
  }
};

export const setToken = (token) => {
  try {
    if (token) {
      localStorage.setItem('ct_token', token);
    } else {
      localStorage.removeItem('ct_token');
    }
  } catch {
    /* bỏ qua lỗi quota */
  }
};

// Tài khoản tab này đang hiển thị. Cookie đăng nhập là chung cho mọi tab, nên server so khớp để từ chối
// thao tác nếu tab khác vừa đăng nhập tài khoản khác hay đăng xuất (lỗi SESSION_CHANGED).
// Bị từ chối quyền (403) có thể do Admin vừa đổi vai trò nên cũng kiểm tra lại phiên.
let SESSION_USER = null;
let onSessionChanged = null;
export function setApiSession(userId, onChanged) {
  SESSION_USER = userId || null;
  onSessionChanged = onChanged;
}

export const appHeaders = () => {
  const headers = {};
  if (SESSION_USER) {
    headers['X-Session-User'] = SESSION_USER;
  }
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

export class ApiError extends Error {
  constructor(status, body) {
    const msg =
      body?.message ||
      body?.error?.message ||
      (status === 401
        ? 'Phiên đăng nhập đã hết hạn. Cô chú vui lòng đăng nhập lại.'
        : status === 403
        ? 'Cô chú không có quyền thực hiện thao tác này.'
        : 'Có lỗi xảy ra, cô chú vui lòng thử lại.');
    super(msg);
    this.status = status;
    this.code = body?.code || body?.error?.code || body?.errorCode;
    this.fields = body?.fields || body?.error?.fields || {};
    this.data = body?.data || body?.error || {};
  }
}

async function request(method, path, body, isRetry = false) {
  let res;
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${API_BASE}/api/v1${cleanPath}`;

  try {
    res = await fetch(url, {
      method,
      credentials: 'include',
      headers: { ...appHeaders(), ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, { message: 'Không kết nối được tới phòng khám. Cô chú kiểm tra mạng rồi thử lại.' });
  }

  const data = res.status === 204 ? null : await res.json().catch(() => null);

  if (!res.ok) {
    // Tự động thử refresh token một lần khi 401 (ngoại trừ các endpoint auth)
    if (res.status === 401 && !isRetry && !cleanPath.startsWith('/auth/')) {
      try {
        const refreshRes = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
          method: 'GET',
          credentials: 'include',
          headers: appHeaders(),
        });
        if (refreshRes.ok) {
          const refreshData = await refreshRes.json().catch(() => null);
          const newToken = refreshData?.data?.token || refreshData?.data?.accessToken || refreshData?.token || refreshData?.accessToken;
          if (newToken) {
            setToken(newToken);
          }
          return await request(method, path, body, true);
        }
      } catch {
        /* Bỏ qua lỗi refresh để chuyển sang xử lý 401 ban đầu */
      }
      setToken(null);
    }

    if (res.status === 401) {
      setToken(null);
    }

    const err = new ApiError(res.status, data);
    if (err.code === 'SESSION_CHANGED' || res.status === 403) {
      onSessionChanged?.();
    }
    throw err;
  }

  // Nếu server trả về token (sau login / verify / refresh), tự động lưu vào localStorage
  const token = data?.data?.token || data?.data?.accessToken || data?.token || data?.accessToken;
  if (token) {
    setToken(token);
  }

  // Nếu là logout, xóa token
  if (cleanPath === '/auth/logout') {
    setToken(null);
  }

  // Bóc tách envelope chuẩn ApiResponse: { success: true, statusCode, code, message, data, timestamp }
  if (data && typeof data === 'object' && 'success' in data && 'data' in data) {
    const payload = data.data;
    if (payload !== null && typeof payload === 'object' && !Array.isArray(payload)) {
      if (data.message && !('message' in payload)) {
        payload.message = data.message;
      }
      if (data.code && !('code' in payload)) {
        payload.code = data.code;
      }
    }
    return payload;
  }

  return data;
}

export function qs(params) {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  return entries.length ? `?${new URLSearchParams(entries)}` : '';
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body = {}) => request('POST', path, body),
  put: (path, body = {}) => request('PUT', path, body),
  patch: (path, body = {}) => request('PATCH', path, body),
  del: (path) => request('DELETE', path),
};
