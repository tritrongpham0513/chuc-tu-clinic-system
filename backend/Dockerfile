# Multi-stage Dockerfile cho Backend Phòng Khám Chúc Từ (Spring Boot 3 + Java 17)

# -------------------------------------------------------------
# Stage 1: Biên dịch mã nguồn với Maven
# -------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependency trước
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Đảm bảo file cấu hình application.yml luôn tồn tại (nếu thiếu sẽ tự động copy từ application.example.yml)
RUN if [ ! -f src/main/resources/application.yml ]; then cp src/main/resources/application.example.yml src/main/resources/application.yml; fi

# Đóng gói file JAR
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Môi trường chạy tối giản JRE 17
# -------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Thiết lập múi giờ Việt Nam và curl cho healthcheck
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

# Chạy bằng user non-root để đảm bảo an toàn
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Lấy file JAR từ stage builder
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 4000

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=5 \
  CMD curl -f http://localhost:4000/api/v1/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
