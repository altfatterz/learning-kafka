package com.github.altfatterz.testcontainersdemo;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// Jupiter / JUnit 5 integration is provided by means of the @Testcontainers annotation.
@Testcontainers
class MixedLifecycleTests {

    // will be shared between test methods
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    // will be started before and stopped after each test method
    @Container
    private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer<>
            (DockerImageName.parse("postgres:16"))
            .withDatabaseName("test").withUsername("postgres").withPassword("secret");

    @Test
    void test1() {
        assertThat(mongoDBContainer.isRunning()).isTrue();
        assertThat(postgresqlContainer.isRunning()).isTrue();
    }

    @Test
    void test2() {
        assertThat(mongoDBContainer.isRunning()).isTrue();
        assertThat(postgresqlContainer.isRunning()).isTrue();
    }

}
