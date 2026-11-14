package com.chuctupk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Phòng Khám Chúc Từ — API v1 Specification")
                        .version("v1.0.0")
                        .description("Tài liệu đặc tả hợp đồng RESTful API chính thức của Hệ thống Quản lý và Tiếp nhận Bệnh nhân Phòng Khám Chúc Từ.")
                        .contact(new Contact()
                                .name("Đội ngũ Kỹ thuật Chúc Từ PK")
                                .email("hotro@chuctu.vn"))
                        .license(new License().name("Nội bộ").url("https://chuctu.vn")))
                .servers(List.of(
                        new Server().url("/").description("Server hiện tại (Cùng domain / reverse proxy)"),
                        new Server().url("http://localhost:4000").description("Môi trường Local Development (Port 4000)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Token xác thực để thực hiện các thao tác có phân quyền")));
    }
}

