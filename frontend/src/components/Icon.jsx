// Bộ icon nét đơn giản dùng trong toàn site (viewBox 24x24, nét tròn).
// Dùng: <Icon name="phone" size={22} stroke="#2E5B37" />

const PATHS = {
  leaf: (
    <>
      <path d="M11 20A7 7 0 0 1 4 13c0-5 4-9 9-10 1 4-1 6-3 8 3 0 5-1 7-3 1 5-1 12-6 12Z" />
    </>
  ),
  'calendar-plus': (
    <>
      <rect x="3" y="5" width="18" height="16" rx="3" />
      <path d="M8 3v4M16 3v4M3 11h18M12 14v4M10 16h4" />
    </>
  ),
  'help-circle': (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.6.2-.9.8-.9 1.4M12 17h.01" />
    </>
  ),
  lock: (
    <>
      <rect x="4" y="10" width="16" height="11" rx="3" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </>
  ),
  heart: (
    <>
      <path d="M20.8 5.6a5 5 0 0 0-7.1 0L12 7.3l-1.7-1.7a5 5 0 1 0-7.1 7.1l1.7 1.7L12 21.5l7.1-7.1 1.7-1.7a5 5 0 0 0 0-7.1Z" />
    </>
  ),
  users: (
    <>
      <path d="M17 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9.5" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.9" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  'arrow-right': (
    <>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </>
  ),
  spine: (
    <>
      <path d="M12 2v20" />
      <circle cx="12" cy="4" r="2" />
      <path d="M12 9c3 0 5-2 5-4M12 15c-3 0-5-2-5-4" />
    </>
  ),
  sprout: (
    <>
      <path d="M4 20c4-1 6-3 7-6M11 14c1-4 4-7 9-8M7 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
    </>
  ),
  hand: (
    <>
      <path d="M8 13V5a2 2 0 1 1 4 0v6M12 11V4a2 2 0 1 1 4 0v8M16 12V7a2 2 0 1 1 4 0v8a6 6 0 0 1-6 6h-2a6 6 0 0 1-6-6v-2a2 2 0 1 1 4 0" />
    </>
  ),
  'pill-jar': (
    <>
      <path d="M9 3h6l1 4H8Z" />
      <path d="M7 7h10l1.5 12a2 2 0 0 1-2 2h-9a2 2 0 0 1-2-2Z" />
      <path d="M10 13h4M12 11v4" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8h.01M11 12h1v4h1" />
    </>
  ),
  'map-pin': (
    <>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </>
  ),
  phone: (
    <>
      <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2Z" />
    </>
  ),
  'check-circle': (
    <>
      <path d="M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z" />
      <path d="m9 12 2 2 4-4" />
    </>
  ),
  check: (
    <>
      <path d="m5 12 5 5L20 7" />
    </>
  ),
  'alert-triangle': (
    <>
      <path d="M12 3 2 20h20L12 3Z" />
      <path d="M12 10v4M12 17h.01" />
    </>
  ),
  send: (
    <>
      <path d="m3 11 19-9-9 19-2-8-8-2Z" />
    </>
  ),
  bike: (
    <>
      <circle cx="5.5" cy="17" r="3" />
      <circle cx="18.5" cy="17" r="3" />
      <path d="M8.5 17h7l-3-7h-3M12.5 10 15 6h3" />
    </>
  ),
  bus: (
    <>
      <rect x="4" y="3" width="16" height="14" rx="3" />
      <path d="M4 10h16M7 20h2M15 20h2M7 17v3M17 17v3" />
      <circle cx="8" cy="14" r="1" />
      <circle cx="16" cy="14" r="1" />
    </>
  ),
  car: (
    <>
      <path d="M5 17h14M4 17v-4l2-5h12l2 5v4M7 17v2M17 17v2" />
      <circle cx="8" cy="13" r="1" />
      <circle cx="16" cy="13" r="1" />
    </>
  ),
  'arrow-left': (
    <>
      <path d="M19 12H5M11 6l-6 6 6 6" />
    </>
  ),
  user: (
    <>
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="3" />
      <path d="M8 3v4M16 3v4M3 11h18" />
    </>
  ),
  'alert-circle': (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v5M12 16h.01" />
    </>
  ),
  'log-in': (
    <>
      <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3" />
    </>
  ),
  'x-2': (
    <>
      <path d="M18 6 6 18M6 6l12 12" />
    </>
  ),
  'user-plus': (
    <>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M19 8v6M22 11h-6" />
    </>
  ),
}

export default function Icon({
  name,
  size = 24,
  stroke = 'currentColor',
  strokeWidth = 1.75,
  fill = 'none',
  join = true, // false: bỏ stroke-linejoin (một vài icon trong thiết kế không có)
  ...rest
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={fill}
      stroke={stroke}
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin={join ? 'round' : undefined}
      aria-hidden="true"
      {...rest}
    >
      {PATHS[name]}
    </svg>
  )
}
