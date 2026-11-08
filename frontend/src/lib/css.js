// Đổi chuỗi CSS ("color: red; font-size: 18px") thành object style của React.
// Dùng cho các style được tính động (vd. chip đang chọn, nhãn chuyên mục).
export function css(str) {
  const style = {}
  String(str || '')
    .split(';')
    .forEach((decl) => {
      const i = decl.indexOf(':')
      if (i < 0) return
      const prop = decl.slice(0, i).trim()
      const value = decl.slice(i + 1).trim()
      if (!prop) return
      const key = prop.startsWith('--') ? prop : prop.replace(/-([a-z])/g, (_, c) => c.toUpperCase())
      style[key] = value
    })
  return style
}
