USE `phongkham_chuctu`;

UPDATE `users` 
SET 
  `password_hash` = '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO',
  `must_change_password` = 0,
  `failed_login_count` = 0,
  `locked_until` = NULL,
  `is_active` = 1
WHERE `username` = 'admin';

USE `phongkham_chuctu_test`;

INSERT INTO `users` (`username`, `password_hash`, `full_name`, `email`, `phone`, `role`, `is_active`, `must_change_password`)
VALUES ('admin', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Quản trị viên', 'admin@chuctu.vn', '0912345678', 'ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE 
  `password_hash` = '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 
  `must_change_password` = 0,
  `is_active` = 1;

