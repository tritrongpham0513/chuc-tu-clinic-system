package com.chuctupk;

import com.chuctupk.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    static {
        DotenvLoader.load();
    }

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(BackendApplication.class, args);
    }
}
