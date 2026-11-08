# Multi-stage Dockerfile cho Frontend Phòng Khám Chúc Từ (React 18 + Vite + Nginx)

# -------------------------------------------------------------
# Stage 1: Build React/Vite
# -------------------------------------------------------------
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency definitions
COPY package*.json ./
RUN npm ci

# Copy toàn bộ mã nguồn frontend
COPY . .

# Khi chạy trong Docker Compose, Nginx đứng trước proxy /api/v1 -> Backend
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# -------------------------------------------------------------
# Stage 2: Nginx Web Server phục vụ static files & Proxy API
# -------------------------------------------------------------
FROM nginx:alpine

# Thay thế cấu hình Nginx mặc định bằng cấu hình SPA và Reverse Proxy API
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy bản build tĩnh từ stage builder vào thư mục web root
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
