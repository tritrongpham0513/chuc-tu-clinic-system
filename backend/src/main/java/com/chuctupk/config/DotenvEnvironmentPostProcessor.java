package com.chuctupk.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tự động nạp file .env vào cấu hình Spring Boot khi khởi động.
 * Tìm kiếm theo thứ tự: thư mục hiện tại (.env), thư mục cha (../.env), hoặc backend/.env.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File[] candidates = new File[] {
                new File(".env"),
                new File("../.env"),
                new File("backend/.env")
        };

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                try {
                    Map<String, Object> props = new HashMap<>();
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String val = line.substring(idx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) ||
                                (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            props.put(key, val);
                            System.setProperty(key, val);
                        }
                    }
                    if (!props.isEmpty()) {
                        environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", props));
                    }
                    break;
                } catch (Exception ignored) {
                    // Bỏ qua lỗi đọc file
                }
            }
        }
    }
}
