-- Migration V12: Thêm index cho tìm kiếm bệnh nhân theo CCCD/CMND và Năm sinh (Sprint 9)
CREATE INDEX idx_patients_national_id ON patients(national_id);
CREATE INDEX idx_patients_year_of_birth ON patients(year_of_birth);
