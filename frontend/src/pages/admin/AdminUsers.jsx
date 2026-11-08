import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import {
  getAdminUsers,
  createAdminUser,
  updateUserRole,
  toggleUserStatus,
  resetUserPassword
} from '../../api/admin'

const ROLE_TABS = [
  { key: 'ALL', label: 'Tất cả tài khoản' },
  { key: 'STAFF', label: '💁‍♀️ Tiếp tân / Lễ tân' },
  { key: 'DOCTOR', label: '👨‍⚕️ Bác sĩ YHCT' },
  { key: 'ADMIN', label: '👑 Quản trị viên' },
  { key: 'PATIENT', label: '👤 Bệnh nhân' },
]

export default function AdminUsers() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('ALL')
  const [search, setSearch] = useState('')

  // Modal thêm tài khoản
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('STAFF')
  const [title, setTitle] = useState('Lương y')
  const [specialty, setSpecialty] = useState('Y học cổ truyền')
  const [submittingCreate, setSubmittingCreate] = useState(false)

  // Modal phân quyền lại
  const [roleTarget, setRoleTarget] = useState(null)
  const [newRole, setNewRole] = useState('STAFF')
  const [newTitle, setNewTitle] = useState('')
  const [newSpecialty, setNewSpecialty] = useState('')

  // Modal đặt lại mật khẩu
  const [pwdTarget, setPwdTarget] = useState(null)
  const [newPassword, setNewPassword] = useState('')

  // 1. Tải danh sách tài khoản
  const loadUsers = useCallback(async () => {
    try {
      const roleParam = activeTab === 'ALL' ? undefined : activeTab
      const res = await getAdminUsers({
        role: roleParam,
        search: search.trim() || undefined
      })
      setUsers(res.data || [])
    } catch (err) {
      console.error('Lỗi tải danh sách tài khoản:', err)
      setUsers([])
    } finally {
      setLoading(false)
    }
  }, [activeTab, search])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  // 2. Thêm tài khoản mới
  const handleCreateUser = async (e) => {
    e.preventDefault()
    setSubmittingCreate(true)
    try {
      const payload = {
        username: username.trim(),
        password: password.trim(),
        fullName: fullName.trim(),
        phone: phone.trim(),
        email: email.trim() || undefined,
        role: role,
        title: role === 'DOCTOR' ? title.trim() : undefined,
        specialty: role === 'DOCTOR' ? specialty.trim() : undefined,
      }
      await createAdminUser(payload)
      alert('✓ Đã tạo tài khoản và phân quyền thành công!')
      setShowCreateModal(false)
      // Reset form
      setUsername('')
      setPassword('')
      setFullName('')
      setPhone('')
      setEmail('')
      loadUsers()
    } catch (err) {
      alert('Lỗi tạo tài khoản: ' + (err.message || 'Lỗi hệ thống'))
    } finally {
      setSubmittingCreate(false)
    }
  }

  // 3. Phân quyền / đổi Role
  const handleUpdateRole = async (e) => {
    e.preventDefault()
    if (!roleTarget) return
    try {
      await updateUserRole(roleTarget.id, {
        role: newRole,
        title: newRole === 'DOCTOR' ? newTitle.trim() : undefined,
        specialty: newRole === 'DOCTOR' ? newSpecialty.trim() : undefined,
      })
      alert(`✓ Đã đổi quyền cho tài khoản ${roleTarget.username} sang ${newRole}!`)
      setRoleTarget(null)
      loadUsers()
    } catch (err) {
      alert('Lỗi phân quyền: ' + err.message)
    }
  }

  // 4. Khóa / Mở khóa
  const handleToggleStatus = async (user) => {
    const nextStatus = !user.isActive
    const actionText = nextStatus ? 'mở khóa' : 'tạm khóa'
    if (!window.confirm(`Bạn có chắc chắn muốn ${actionText} tài khoản "${user.username}"?`)) return

    try {
      await toggleUserStatus(user.id, nextStatus)
      loadUsers()
    } catch (err) {
      alert('Lỗi thay đổi trạng thái: ' + err.message)
    }
  }

  // 5. Đặt lại mật khẩu
  const handleResetPassword = async (e) => {
    e.preventDefault()
    if (!pwdTarget || !newPassword) return
    try {
      await resetUserPassword(pwdTarget.id, newPassword.trim())
      alert(`✓ Đã đổi mật khẩu mới cho tài khoản ${pwdTarget.username}!`)
      setPwdTarget(null)
      setNewPassword('')
    } catch (err) {
      alert('Lỗi đổi mật khẩu: ' + err.message)
    }
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1280, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 14 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 26, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              👥 Quản Lý Tài Khoản & Phân Quyền Nhân Sự
            </h1>
            <div style={{ fontSize: 15, color: '#6B5E4E', marginTop: 4 }}>
              Độc quyền Quản trị viên: Cấp tài khoản và chỉ định quyền cho Tiếp tân, Bác sĩ YHCT
            </div>
          </div>

          <button
            onClick={() => setShowCreateModal(true)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              minHeight: 46, padding: '0 20px', borderRadius: 12,
              background: '#2D5A27', color: '#fff', border: 'none',
              fontSize: 16, fontWeight: 700, cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(45,90,39,0.25)'
            }}
          >
            + Cấp tài khoản nhân sự mới
          </button>
        </div>

        {/* Lọc theo Role & Search */}
        <div style={{ background: '#fff', padding: '16px 20px', borderRadius: 16, border: '1px solid #E0D3BC', marginBottom: 20 }}>
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
            {/* Tabs */}
            <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
              {ROLE_TABS.map(tab => {
                const isActive = activeTab === tab.key
                return (
                  <button
                    key={tab.key}
                    onClick={() => setActiveTab(tab.key)}
                    style={{
                      padding: '8px 16px', borderRadius: 10, border: 'none',
                      background: isActive ? '#2D5A27' : '#F5EFE6',
                      color: isActive ? '#fff' : '#5C3A21',
                      fontSize: 15, fontWeight: isActive ? 700 : 600,
                      cursor: 'pointer', whiteSpace: 'nowrap'
                    }}
                  >
                    {tab.label}
                  </button>
                )
              })}
            </div>

            {/* Ô tìm kiếm */}
            <div style={{ minWidth: 280, flex: '1 1 300px' }}>
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="🔍 Tìm theo Tên đăng nhập, Họ tên, SĐT..."
                style={{
                  width: '100%', minHeight: 42, padding: '0 14px',
                  borderRadius: 10, border: '1.5px solid #E0D3BC',
                  fontSize: 15, outline: 'none', boxSizing: 'border-box'
                }}
              />
            </div>
          </div>
        </div>

        {/* Bảng danh sách tài khoản */}
        <div style={{ background: '#fff', borderRadius: 18, border: '1px solid #E0D3BC', overflow: 'hidden', boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#6B5E4E', fontSize: 17 }}>⏳ Đang tải danh sách tài khoản...</div>
          ) : users.length === 0 ? (
            <div style={{ padding: 48, textAlign: 'center', color: '#8C7B5E' }}>
              <div style={{ fontSize: 40, marginBottom: 8 }}>📭</div>
              <div style={{ fontSize: 17, fontWeight: 600 }}>Không tìm thấy tài khoản nào</div>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 960 }}>
                <thead>
                  <tr style={{ background: '#F5EFE6', color: '#5C3A21', fontSize: 15, borderBottom: '2px solid #E0D3BC' }}>
                    <th style={{ padding: '14px 18px' }}>Tên đăng nhập</th>
                    <th style={{ padding: '14px 18px' }}>Họ và tên</th>
                    <th style={{ padding: '14px 18px' }}>Số điện thoại</th>
                    <th style={{ padding: '14px 18px' }}>Vai trò (Phân quyền)</th>
                    <th style={{ padding: '14px 18px' }}>Trạng thái</th>
                    <th style={{ padding: '14px 18px', textAlign: 'center' }}>Thao tác quản trị</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u, idx) => {
                    const isStaff = u.role === 'STAFF'
                    const isDoctor = u.role === 'DOCTOR'
                    const isAdmin = u.role === 'ADMIN'
                    const isPatient = u.role === 'PATIENT'

                    return (
                      <tr key={u.id} style={{ borderBottom: '1px solid #F0E8DC', background: idx % 2 === 0 ? '#fff' : '#FDFAF5' }}>
                        {/* Username */}
                        <td style={{ padding: '14px 18px' }}>
                          <span style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: 16, color: '#1F1610' }}>
                            {u.username}
                          </span>
                        </td>

                        {/* Họ tên */}
                        <td style={{ padding: '14px 18px' }}>
                          <div style={{ fontSize: 16, fontWeight: 700, color: '#2B1A0F' }}>{u.fullName}</div>
                          {u.doctorSpecialty && (
                            <div style={{ fontSize: 13, color: '#15803D' }}>
                              {u.doctorTitle || 'Bác sĩ'}: {u.doctorSpecialty}
                            </div>
                          )}
                        </td>

                        {/* SĐT */}
                        <td style={{ padding: '14px 18px', fontSize: 15, color: '#4B5563' }}>
                          📞 {u.phone}
                        </td>

                        {/* Vai trò */}
                        <td style={{ padding: '14px 18px' }}>
                          {isAdmin && (
                            <span style={{ background: '#FEE2E2', color: '#991B1B', padding: '4px 10px', borderRadius: 8, fontSize: 13, fontWeight: 800 }}>
                              👑 Quản trị viên
                            </span>
                          )}
                          {isStaff && (
                            <span style={{ background: '#DBEAFE', color: '#1E40AF', padding: '4px 10px', borderRadius: 8, fontSize: 13, fontWeight: 800 }}>
                              💁‍♀️ Tiếp tân / Lễ tân
                            </span>
                          )}
                          {isDoctor && (
                            <span style={{ background: '#DCFCE7', color: '#166534', padding: '4px 10px', borderRadius: 8, fontSize: 13, fontWeight: 800 }}>
                              👨‍⚕️ Bác sĩ YHCT
                            </span>
                          )}
                          {isPatient && (
                            <span style={{ background: '#F3F4F6', color: '#4B5563', padding: '4px 10px', borderRadius: 8, fontSize: 13, fontWeight: 700 }}>
                              👤 Bệnh nhân
                            </span>
                          )}
                        </td>

                        {/* Trạng thái */}
                        <td style={{ padding: '14px 18px' }}>
                          {u.isActive ? (
                            <span style={{ color: '#15803D', fontWeight: 700, fontSize: 14 }}>● Đang hoạt động</span>
                          ) : (
                            <span style={{ color: '#DC2626', fontWeight: 700, fontSize: 14 }}>🔒 Đang bị khóa</span>
                          )}
                        </td>

                        {/* Thao tác */}
                        <td style={{ padding: '14px 18px', textAlign: 'center' }}>
                          <div style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
                            {/* Phân quyền lại */}
                            <button
                              onClick={() => {
                                setRoleTarget(u)
                                setNewRole(u.role)
                                setNewTitle(u.doctorTitle || 'Lương y')
                                setNewSpecialty(u.doctorSpecialty || 'Y học cổ truyền')
                              }}
                              style={{
                                background: '#F5EFE6', color: '#5C3A21', border: '1px solid #D6C7B2',
                                padding: '5px 12px', borderRadius: 8, fontSize: 13, fontWeight: 700, cursor: 'pointer'
                              }}
                            >
                              Phân quyền
                            </button>

                            {/* Đổi mật khẩu */}
                            <button
                              onClick={() => { setPwdTarget(u); setNewPassword('') }}
                              style={{
                                background: '#EFF6FF', color: '#1D4ED8', border: '1px solid #BFDBFE',
                                padding: '5px 10px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer'
                              }}
                            >
                              Đổi MK
                            </button>

                            {/* Khóa / Mở khóa (không cho khóa admin gốc) */}
                            {u.username !== 'admin' && (
                              <button
                                onClick={() => handleToggleStatus(u)}
                                style={{
                                  background: u.isActive ? '#FFF1F2' : '#F0FDF4',
                                  color: u.isActive ? '#DC2626' : '#15803D',
                                  border: `1px solid ${u.isActive ? '#FECDD3' : '#BBF7D0'}`,
                                  padding: '5px 10px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer'
                                }}
                              >
                                {u.isActive ? 'Khóa' : 'Mở'}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* MODAL 1: CẤP TÀI KHOẢN MỚI */}
        {showCreateModal && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 20, width: '100%', maxWidth: 520, padding: 28, boxShadow: '0 20px 40px rgba(0,0,0,0.2)', border: '2px solid #E0D3BC' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
                <h2 style={{ margin: 0, fontSize: 22, color: '#2B1A0F', fontFamily: "'Noto Serif', serif" }}>
                  👤 Cấp tài khoản nhân sự & Phân quyền
                </h2>
                <button onClick={() => setShowCreateModal(false)} style={{ border: 'none', background: 'none', fontSize: 22, cursor: 'pointer' }}>✕</button>
              </div>

              <form onSubmit={handleCreateUser} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Tên đăng nhập (*)</label>
                    <input
                      type="text"
                      required
                      value={username}
                      onChange={e => setUsername(e.target.value)}
                      placeholder="VD: bs_minh, letan_hoa"
                      style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Mật khẩu (*)</label>
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={e => setPassword(e.target.value)}
                      placeholder="Tối thiểu 6 ký tự"
                      style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Họ và tên (*)</label>
                    <input
                      type="text"
                      required
                      value={fullName}
                      onChange={e => setFullName(e.target.value)}
                      placeholder="VD: Lương y Trần Văn Tuấn"
                      style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Số điện thoại (*)</label>
                    <input
                      type="tel"
                      required
                      value={phone}
                      onChange={e => setPhone(e.target.value)}
                      placeholder="10 chữ số..."
                      style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                {/* Chọn vai trò */}
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 700, color: '#2D5A27', marginBottom: 6 }}>
                    Phân quyền vai trò (Role) (*)
                  </label>
                  <select
                    value={role}
                    onChange={e => setRole(e.target.value)}
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '2px solid #2D5A27', fontSize: 15, fontWeight: 700, color: '#2D5A27' }}
                  >
                    <option value="STAFF">💁‍♀️ Tiếp tân / Lễ tân (Bàn tiếp đón, cấp STT, check-in)</option>
                    <option value="DOCTOR">👨‍⚕️ Bác sĩ YHCT (Bàn khám bệnh, bắt mạch, kê đơn thuốc)</option>
                    <option value="ADMIN">👑 Quản trị viên (Toàn quyền hệ thống & phân quyền)</option>
                  </select>
                </div>

                {/* Nếu chọn Bác sĩ -> hiện chức danh & chuyên khoa */}
                {role === 'DOCTOR' && (
                  <div style={{ background: '#F0FDF4', border: '1.5px solid #86EFAC', borderRadius: 12, padding: 14 }}>
                    <div style={{ fontSize: 14, fontWeight: 700, color: '#166534', marginBottom: 8 }}>
                      🩺 THÔNG TIN BUỒNG KHÁM BÁC SĨ:
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                      <div>
                        <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Chức danh / Học vị</label>
                        <input
                          type="text"
                          value={title}
                          onChange={e => setTitle(e.target.value)}
                          placeholder="Lương y / Bác sĩ CKI..."
                          style={{ width: '100%', minHeight: 38, padding: '0 8px', borderRadius: 8, border: '1px solid #86EFAC', fontSize: 14, boxSizing: 'border-box' }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Chuyên khoa phụ trách</label>
                        <input
                          type="text"
                          value={specialty}
                          onChange={e => setSpecialty(e.target.value)}
                          placeholder="Cơ xương khớp / Mất ngủ..."
                          style={{ width: '100%', minHeight: 38, padding: '0 8px', borderRadius: 8, border: '1px solid #86EFAC', fontSize: 14, boxSizing: 'border-box' }}
                        />
                      </div>
                    </div>
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}>
                  <button type="button" onClick={() => setShowCreateModal(false)} style={{ padding: '8px 16px', borderRadius: 8, border: '1px solid #ccc' }}>Đóng</button>
                  <button
                    type="submit"
                    disabled={submittingCreate}
                    style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#2D5A27', color: '#fff', fontWeight: 700, cursor: 'pointer' }}
                  >
                    {submittingCreate ? 'Đang tạo...' : '✓ Xác nhận cấp tài khoản'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL 2: PHÂN QUYỀN LẠI */}
        {roleTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 18, width: '100%', maxWidth: 440, padding: 24 }}>
              <h3 style={{ margin: '0 0 12px', fontSize: 19, color: '#2B1A0F' }}>
                Thay đổi phân quyền: {roleTarget.fullName} ({roleTarget.username})
              </h3>
              <form onSubmit={handleUpdateRole} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Chọn vai trò mới</label>
                  <select
                    value={newRole}
                    onChange={e => setNewRole(e.target.value)}
                    style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, fontWeight: 700 }}
                  >
                    <option value="STAFF">💁‍♀️ Tiếp tân / Lễ tân</option>
                    <option value="DOCTOR">👨‍⚕️ Bác sĩ YHCT</option>
                    <option value="ADMIN">👑 Quản trị viên</option>
                    <option value="PATIENT">👤 Bệnh nhân</option>
                  </select>
                </div>

                {newRole === 'DOCTOR' && (
                  <div style={{ background: '#F0FDF4', padding: 12, borderRadius: 8, border: '1px solid #BBF7D0' }}>
                    <div style={{ marginBottom: 8 }}>
                      <label style={{ display: 'block', fontSize: 13, fontWeight: 600 }}>Chức danh</label>
                      <input
                        type="text"
                        value={newTitle}
                        onChange={e => setNewTitle(e.target.value)}
                        placeholder="Lương y / Bác sĩ CKI..."
                        style={{ width: '100%', minHeight: 36, padding: '0 8px', borderRadius: 6, border: '1px solid #ccc', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: 13, fontWeight: 600 }}>Chuyên khoa</label>
                      <input
                        type="text"
                        value={newSpecialty}
                        onChange={e => setNewSpecialty(e.target.value)}
                        placeholder="Cơ xương khớp / Châm cứu..."
                        style={{ width: '100%', minHeight: 36, padding: '0 8px', borderRadius: 6, border: '1px solid #ccc', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
                  <button type="button" onClick={() => setRoleTarget(null)} style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #ccc' }}>Hủy</button>
                  <button type="submit" style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#2D5A27', color: '#fff', fontWeight: 700 }}>Lưu phân quyền</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL 3: ĐẶT LẠI MẬT KHẨU */}
        {pwdTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 18, width: '100%', maxWidth: 420, padding: 24 }}>
              <h3 style={{ margin: '0 0 12px', fontSize: 19, color: '#2B1A0F' }}>
                Đổi mật khẩu cho: {pwdTarget.username}
              </h3>
              <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 4 }}>Mật khẩu mới (*)</label>
                  <input
                    type="password"
                    required
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    placeholder="Nhập tối thiểu 6 ký tự..."
                    style={{ width: '100%', minHeight: 42, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                  />
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
                  <button type="button" onClick={() => setPwdTarget(null)} style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #ccc' }}>Hủy</button>
                  <button type="submit" style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#2563EB', color: '#fff', fontWeight: 700 }}>Đổi mật khẩu</button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}

