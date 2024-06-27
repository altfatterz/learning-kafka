package com.github.altfatterz.testcontainersdemo.singleton;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

// Singleton Container Pattern
// https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers

@Testcontainers
abstract class AbstractContainerBaseTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }

}
