---
name: clinic-booking-workflow
description: >-
  Áp dụng khi lập trình logic, API hoặc luồng giao diện cho chức năng Đặt lịch khám bệnh,
  quản lý ca khám của bác sĩ và hồ sơ bệnh nhân tại Phòng Khám Chúc Từ.
---

# Quy Trình Nghiệp Vụ Đặt Lịch Khám (Clinic Booking Workflow)

## 1. Các bước trong luồng đặt lịch (User Journey)
1. **Bước 1: Chọn chuyên khoa hoặc Bác sĩ phụ trách**.
2. **Bước 2: Chọn ngày và khung giờ (Time slot)**:
   - Khung giờ sáng: 08:00 - 11:30 (Mỗi slot 30 phút).
   - Khung giờ chiều: 13:30 - 17:00 (Mỗi slot 30 phút).
3. **Bước 3: Nhập thông tin bệnh nhân**:
   - Họ và tên, Năm sinh, Số điện thoại, Triệu chứng/Lý do khám.
4. **Bước 4: Xác nhận & Cấp mã lịch hẹn (Booking Code)**:
   - Sinh mã dạng: `PKCT-YYYYMMDD-XXXX` (VD: `PKCT-20261014-0821`).
5. **Bước 5: Gửi thông báo xác nhận**:
   - Gửi SMS/Zalo/Email xác nhận kèm giờ hẹn và địa chỉ phòng khám.

## 2. Ràng buộc kỹ thuật (Validation & Business Rules)
- Không cho phép đặt lịch vào các khung giờ trong quá khứ.
- Bác sĩ đang bận ca khám hoặc đã đủ 3 người/slot thì phải hiển thị trạng thái `Đầy ca` (Disabled slot).
- Khi hủy lịch phải lưu lý do hủy và cập nhật thời gian hủy.
