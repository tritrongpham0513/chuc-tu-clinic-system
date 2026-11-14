package com.chuctupk.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Tự động nạp file .env vào cấu hình Spring Boot khi khởi động qua cơ chế EnvironmentPostProcessor.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = DotenvLoader.load();
        if (!props.isEmpty() && environment != null) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", props));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
