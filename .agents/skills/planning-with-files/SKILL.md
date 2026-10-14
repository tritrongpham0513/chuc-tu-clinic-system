---
name: planning-with-files
description: >-
  Áp dụng cho các tác vụ phức tạp, kéo dài nhiều bước (từ 3 bước trở lên) hoặc khi bắt đầu phiên làm việc mới.
  Cơ chế lưu trữ trí nhớ bền vững trên ổ đĩa (Manus-style) thông qua bộ 3 file: task_plan.md, findings.md, progress.md.
  Ngăn chặn mất ngữ cảnh (context rot) khi khởi động lại hoặc sau các phiên chat dài.
---

# Persistent Planning With Files (Manus Architecture)

Bộ quy chuẩn duy trì trí nhớ làm việc ngoại vi (external memory) trên ổ cứng, giúp AI không bao giờ bị quên tiến độ dự án.

---

## 1. Bộ 3 File Trí Nhớ Cốt Lõi (Nằm ở thư mục gốc dự án)

1. **`task_plan.md` (Kế hoạch hành động & Checklist)**
   - Chứa mục tiêu chính của dự án/tính năng.
   - Các giai đoạn (Phases) và danh sách công việc cần làm dạng `- [ ]` (chưa làm), `- [/]` (đang làm), `- [x]` (đã hoàn thành).
   - Mỗi khi xong một việc, AI **phải cập nhật** ngay file này.

2. **`findings.md` (Sổ tay phát hiện & Quy tắc)**
   - Lưu lại các quyết định kỹ thuật quan trọng (ví dụ: chọn thư viện gì, cổng port bao nhiêu).
   - Lưu lại các lỗi đã gặp và cách sửa để không bao giờ mắc lại lần 2.
   - Lưu lại các ràng buộc nghiệp vụ (ví dụ: quy định đặt lịch của phòng khám).

3. **`progress.md` (Nhật ký tiến độ theo thời gian)**
   - Ghi lại mốc thời gian: Ngày nào, giờ nào đã hoàn thành tính năng gì.
   - Trạng thái hiện tại: Đang dừng ở dòng code nào hoặc chức năng nào.
   - Bước tiếp theo ngay khi người dùng quay trở lại là gì.

---

## 2. Quy Trình Làm Việc Bắt Buộc Của AI

* **BƯỚC 1 - Đọc trí nhớ cũ**: Trước khi bắt đầu một phiên làm việc, luôn kiểm tra xem `task_plan.md` và `progress.md` đã có chưa. Nếu có, đọc nội dung để nắm bắt tiến độ hiện tại.
* **BƯỚC 2 - Cập nhật liên tục**: Không giữ tiến độ trong "đầu" (bộ nhớ chat), luôn ghi ra đĩa sau mỗi bước hoàn tất.
* **BƯỚC 3 - Cổng kiểm duyệt hoàn thành**: Chỉ được báo cho người dùng là "đã hoàn thành" khi tất cả các ô `- [ ]` trong `task_plan.md` đã được tích `- [x]` và kiểm tra không còn lỗi.
