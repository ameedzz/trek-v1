package com.trek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Trek v1 — Reddit Search Engine
 *
 * Run: mvn spring-boot:run
 * API: http://localhost:8080/api/search?q=machine+learning
 *
 * Offline mode (default): uses 30 mock Reddit posts
 * Live mode: set trek.offline=false to pull from PullPush
 */
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
