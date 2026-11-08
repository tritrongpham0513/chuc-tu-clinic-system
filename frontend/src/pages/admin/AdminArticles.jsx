import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import { getAdminArticles, createAdminArticle, updateAdminArticle, deleteAdminArticle } from '../../api/admin'
import { getArticleCategories } from '../../api/article'

export default function AdminArticles() {
  const [articles, setArticles] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)

  // Modal
  const [showModal, setShowModal] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [form, setForm] = useState({
    title: '', summary: '', content: '', thumbnailUrl: '', categoryId: '', isPublished: true
  })
  const [saving, setSaving] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)
  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAdminArticles({ page: 0, size: 50 })
      setArticles(res.data?.content || [])
    } catch {
      setArticles([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
    getArticleCategories().then(r => {
      setCategories(r.data || [])
      if (r.data?.length > 0 && !form.categoryId) {
        setForm(f => ({ ...f, categoryId: r.data[0].id }))
      }
    }).catch(() => {})
  }, [loadData])

  const openAdd = () => {
    setEditTarget(null)
    setForm({
      title: '', summary: '', content: '', thumbnailUrl: '',
      categoryId: categories[0]?.id || '', isPublished: true
    })
    setShowModal(true)
  }

  const openEdit = (a) => {
    setEditTarget(a)
    setForm({
      title: a.title || '',
      summary: a.summary || '',
      content: a.content || '',
      thumbnailUrl: a.thumbnailUrl || '',
      categoryId: a.categoryId || categories[0]?.id || '',
      isPublished: Boolean(a.isPublished)
    })
    setShowModal(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = {
        ...form,
        categoryId: form.categoryId ? parseInt(form.categoryId, 10) : undefined
      }
      if (editTarget) {
        await updateAdminArticle(editTarget.id, payload)
        showToast('Cập nhật bài viết thành công!')
      } else {
        await createAdminArticle(payload)
        showToast('Đăng bài viết mới thành công!')
      }
      setShowModal(false)
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra', 'err')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id, title) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa bài viết "${title}" không?`)) return
    try {
      await deleteAdminArticle(id)
      showToast('Đã xóa bài viết')
      loadData()
    } catch {
      showToast('Không thể xóa bài viết này', 'err')
    }
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              📰 Cẩm nang Y học cổ truyền (CMS)
            </h1>
            <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
              Đăng tải mẹo chữa bệnh dân gian, kiến thức xoa bóp bấm huyệt và thảo dược
            </div>
          </div>

          <button
            onClick={openAdd}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              minHeight: 48, padding: '0 22px', borderRadius: 12,
              background: '#2D5A27', color: '#fff', fontSize: 17, fontWeight: 700, border: 'none', cursor: 'pointer'
            }}
          >
            + Viết bài mới
          </button>
        </div>

        {/* Danh sách bài viết */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#6B5E4E', fontSize: 18 }}>⏳ Đang tải bài viết...</div>
        ) : articles.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>✍️</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#2B1A0F', marginBottom: 6 }}>Chưa có bài viết nào</div>
            <div style={{ fontSize: 16, color: '#6B5E4E' }}>Bấm nút "+ Viết bài mới" để tạo bài cẩm nang sức khỏe đầu tiên.</div>
          </div>
        ) : (
          <div style={{ background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 800 }}>
              <thead>
                <tr style={{ background: '#F8F5EE', borderBottom: '1.5px solid #E0D3BC', color: '#5C3A21', fontSize: 15, fontWeight: 700 }}>
                  <th style={{ padding: '14px 16px' }}>Tiêu đề bài viết</th>
                  <th style={{ padding: '14px 16px' }}>Chuyên mục</th>
                  <th style={{ padding: '14px 16px' }}>Trạng thái</th>
                  <th style={{ padding: '14px 16px' }}>Lượt xem</th>
                  <th style={{ padding: '14px 16px', textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {articles.map((a) => (
                  <tr key={a.id} style={{ borderBottom: '1px solid #F0EAE0', fontSize: 16 }}>
                    <td style={{ padding: '14px 16px', maxWidth: 360 }}>
                      <div style={{ fontWeight: 700, color: '#1F1610' }}>{a.title}</div>
                      <div style={{ fontSize: 13, color: '#8C7B5E', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {a.summary}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ background: '#F0EAE0', padding: '4px 10px', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
                        {a.categoryName || 'Mẹo dân gian'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                        padding: '4px 10px', borderRadius: 99, fontSize: 13, fontWeight: 700,
                        background: a.isPublished ? '#E4EFE2' : '#FFF0EE',
                        color: a.isPublished ? '#2D5A27' : '#90372A'
                      }}>
                        {a.isPublished ? 'Đã xuất bản' : 'Bản nháp'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: '#5C3A21' }}>
                      👁️ {a.viewCount || 0}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: 8 }}>
                        <button
                          onClick={() => openEdit(a)}
                          style={{ padding: '6px 12px', borderRadius: 8, background: '#fff', border: '1px solid #E0D3BC', color: '#5C3A21', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => handleDelete(a.id, a.title)}
                          style={{ padding: '6px 10px', borderRadius: 8, background: 'transparent', border: '1px solid #90372A40', color: '#90372A', fontSize: 14, cursor: 'pointer' }}
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ─── MODAL THÊM / SỬA BÀI VIẾT ─── */}
        {showModal && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 300, padding: 16 }}>
            <div style={{ background: '#FDFBF7', borderRadius: 20, padding: '28px 24px', width: '100%', maxWidth: 700, maxHeight: '92vh', overflowY: 'auto' }}>
              <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#5C3A21', marginBottom: 18 }}>
                {editTarget ? '✏️ Chỉnh sửa bài viết' : '➕ Viết bài cẩm nang mới'}
              </h2>

              <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Tiêu đề bài viết *</label>
                  <input
                    type="text" required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })}
                    placeholder="Ví dụ: 5 mẹo dân gian xoa bóp giảm đau khớp gối cho người già"
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Danh mục *</label>
                    <select
                      value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Trạng thái</label>
                    <select
                      value={form.isPublished ? 'true' : 'false'} onChange={e => setForm({ ...form, isPublished: e.target.value === 'true' })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      <option value="true">Xuất bản ngay</option>
                      <option value="false">Lưu bản nháp</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Tóm tắt ngắn (dưới 200 chữ) *</label>
                  <textarea
                    rows={2} required value={form.summary} onChange={e => setForm({ ...form, summary: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Nội dung chi tiết *</label>
                  <textarea
                    rows={8} required value={form.content} onChange={e => setForm({ ...form, content: e.target.value })}
                    placeholder="Nhập nội dung chia sẻ y học cổ truyền, cách dùng thảo dược..."
                    style={{ width: '100%', padding: '12px 14px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 12 }}>
                  <button type="button" onClick={() => setShowModal(false)} style={{ padding: '0 18px', minHeight: 46, borderRadius: 10, background: '#fff', border: '1.5px solid #E0D3BC', fontSize: 16, cursor: 'pointer' }}>
                    Hủy bỏ
                  </button>
                  <button type="submit" disabled={saving} style={{ padding: '0 24px', minHeight: 46, borderRadius: 10, background: '#2D5A27', color: '#fff', border: 'none', fontSize: 16, fontWeight: 700, cursor: 'pointer' }}>
                    {saving ? 'Đang lưu...' : 'Lưu bài viết'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Toast */}
        {toast && (
          <div style={{ position: 'fixed', bottom: 32, right: 32, background: toast.type === 'err' ? '#90372A' : '#2D5A27', color: '#fff', padding: '14px 24px', borderRadius: 14, fontSize: 16, fontWeight: 600, zIndex: 999 }}>
            {toast.type === 'err' ? '❌' : '✅'} {toast.msg}
          </div>
        )}
      </div>
    </AdminLayout>
  )
}

