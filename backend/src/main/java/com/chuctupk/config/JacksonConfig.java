package com.chuctupk.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;

/**
 * Cấu hình Jackson toàn cục (Spring Boot 4 dùng Jackson 3 – package {@code tools.jackson}):
 * 1. Chuyển đổi toàn bộ kiểu Long / BigInteger sang chuỗi JSON (tránh tràn số trên client JS và đồng nhất kiểu ID).
 * 2. Giữ nguyên các trường kiểu int, Integer (như năm sinh, mã trạng thái HTTP, số lượng) dưới dạng số.
 *
 * <p>Lưu ý: ở Spring Boot 4, cách đăng ký module là khai báo bean
 * {@link JsonMapperBuilderCustomizer} để Spring Boot gắn module vào {@code JsonMapper} chuẩn
 * của ứng dụng. Khai báo {@code SimpleModule} dạng bean đơn thuần sẽ KHÔNG được áp dụng.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.addModule(longToStringModule());
    }

    private SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule("LongToStringModule");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        return module;
    }
}
