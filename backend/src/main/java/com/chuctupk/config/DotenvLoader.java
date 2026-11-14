package com.chuctupk.config;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nạp tệp cấu hình .env vào System Properties và Spring Environment.
 * Đảm bảo ứng dụng chạy độc lập từ IDE, terminal (mvn spring-boot:run), hay test runner
 * mà không yêu cầu người dùng phải tự export biến môi trường thủ công.
 */
public final class DotenvLoader {

    private static volatile boolean loaded = false;
    private static final Map<String, Object> CACHED_PROPERTIES = new LinkedHashMap<>();

    private DotenvLoader() {}

    public static synchronized Map<String, Object> load() {
        if (loaded) {
            return Collections.unmodifiableMap(CACHED_PROPERTIES);
        }

        Path userDir = Paths.get(System.getProperty("user.dir", "."));
        Path[] candidates = new Path[]{
                Paths.get(".env"),
                Paths.get("..", ".env"),
                userDir.resolve(".env"),
                userDir.resolve("..").resolve(".env"),
                userDir.resolve("backend").resolve(".env")
        };

        for (Path p : candidates) {
            try {
                Path normalized = p.toAbsolutePath().normalize();
                if (Files.isRegularFile(normalized)) {
                    try (BufferedReader reader = Files.newBufferedReader(normalized, StandardCharsets.UTF_8)) {
                        String line;
                        int count = 0;
                        while ((line = reader.readLine()) != null) {
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
                                CACHED_PROPERTIES.put(key, val);
                                if (System.getProperty(key) == null && System.getenv(key) == null) {
                                    System.setProperty(key, val);
                                    count++;
                                }
                            }
                        }
                        System.out.println("[DotenvLoader] Đã nạp " + count + " biến môi trường từ " + normalized);
                        loaded = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[DotenvLoader] Lỗi đọc .env tại " + p + ": " + e.getMessage());
            }
        }

        return Collections.unmodifiableMap(CACHED_PROPERTIES);
    }
}
