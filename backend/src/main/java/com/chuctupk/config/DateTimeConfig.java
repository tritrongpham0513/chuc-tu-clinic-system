package com.chuctupk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cấu hình Clock múi giờ Việt Nam (Asia/Ho_Chi_Minh) phục vụ tính toán thời gian,
 * cho phép dễ dàng mock thời gian trong các bài test.
 */
@Configuration
public class DateTimeConfig {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(VIETNAM_ZONE);
    }
}

