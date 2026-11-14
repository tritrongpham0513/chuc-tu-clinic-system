package com.chuctupk.architecture;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureConventionTest {

    private static final String BASE_PACKAGE = "com.chuctupk";

    @Test
    @DisplayName("Kiến trúc phân lớp: Controller KHÔNG được trực tiếp phụ thuộc vào Repository")
    void testControllersDoNotDependDirectlyOnRepositories() throws Exception {
        List<Class<?>> controllerClasses = getClassesInPackage(BASE_PACKAGE + ".controller");
        assertThat(controllerClasses).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Class<?> controllerClass : controllerClasses) {
            if (!controllerClass.isAnnotationPresent(RestController.class)) {
                continue;
            }

            // Bỏ qua các controller compat cũ nếu có
            if (controllerClass.getPackageName().contains(".compat")) {
                continue;
            }

            for (Field field : controllerClass.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (Repository.class.isAssignableFrom(fieldType) || JpaRepository.class.isAssignableFrom(fieldType)) {
                    violations.add(controllerClass.getSimpleName() + " contains repository field: " + field.getName());
                }
                if (fieldType.getPackageName().startsWith(BASE_PACKAGE + ".repository")) {
                    violations.add(controllerClass.getSimpleName() + " contains repository field: " + field.getName());
                }
            }
        }

        assertThat(violations)
                .as("Controllers must not directly depend on Repositories (Layered Architecture violation)")
                .isEmpty();
    }

    @Test
    @DisplayName("Thực thể dữ liệu (Entities): Bắt buộc có chú thích @Entity")
    void testEntitiesHaveEntityAnnotation() throws Exception {
        List<Class<?>> entityClasses = getClassesInPackage(BASE_PACKAGE + ".entity");
        assertThat(entityClasses).isNotEmpty();

        for (Class<?> clazz : entityClasses) {
            if (clazz.isEnum() || clazz.isInterface() || clazz.isAnonymousClass() || clazz.getSimpleName().endsWith("Id")) {
                continue;
            }
            assertThat(clazz.isAnnotationPresent(Entity.class))
                    .as("Class " + clazz.getSimpleName() + " in entity package must have @Entity annotation")
                    .isTrue();
        }
    }

    private List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            return classes;
        }

        File directory = new File(resource.toURI());
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class") && !file.getName().contains("$")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        classes.add(Class.forName(className));
                    }
                }
            }
        }
        return classes;
    }
}

