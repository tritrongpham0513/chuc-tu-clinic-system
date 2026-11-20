-- V7__allow_null_doctor_user_id.sql: Cho phép bác sĩ chưa liên kết tài khoản hệ thống (user_id NULL)
SET NAMES utf8mb4;

ALTER TABLE `doctors` MODIFY COLUMN `user_id` BIGINT UNSIGNED NULL;
