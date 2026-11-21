package com.chuctupk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tự động chạy migration bổ sung cột CSDL còn thiếu TRƯỚC KHI Hibernate validate schema.
 * Đảm bảo hệ thống khởi động thành công trên mọi môi trường Cloud (TiDB Cloud, Aiven, Render)
 * ngay cả khi quản trị viên chưa kịp chạy script migration 008 thủ công.
 */
@Slf4j
@Component
public class DatabaseAutoMigrationListener implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private boolean migrationExecuted = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!migrationExecuted && bean instanceof AbstractEntityManagerFactoryBean) {
            executeAutoMigration();
            migrationExecuted = true;
        }
        return bean;
    }

    public void executeAutoMigration() {
        try {
            DataSource dataSource = applicationContext.getBean(DataSource.class);
            try (Connection conn = dataSource.getConnection()) {
                // 1. Kiểm tra bảng appointments và cột reschedule_count
                boolean hasRescheduleCount = false;
                boolean tableAppointmentsExists = false;

                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'appointments'")) {
                        if (rs.next()) {
                            tableAppointmentsExists = true;
                        }
                    }
                    if (tableAppointmentsExists) {
                        try (ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM `appointments` LIKE 'reschedule_count'")) {
                            if (rs.next()) {
                                hasRescheduleCount = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Không thể kiểm tra cột reschedule_count qua SHOW COLUMNS: {}", e.getMessage());
                }

                if (tableAppointmentsExists && !hasRescheduleCount) {
                    log.info("[Auto-Migration] Phát hiện CSDL thiếu cột 'reschedule_count' trong bảng 'appointments'. Đang tự động bổ sung...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `appointments` ADD COLUMN `reschedule_count` INT NOT NULL DEFAULT 0 COMMENT 'Số lần đổi lịch hẹn'");
                        log.info("[Auto-Migration] Đã bổ sung thành công cột 'reschedule_count' vào bảng 'appointments'.");
                    } catch (Exception e) {
                        log.warn("[Auto-Migration] Lỗi khi thêm cột reschedule_count: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Auto-Migration] Không thể tự động chạy migration khởi động: {}", e.getMessage());
        }
    }
}
