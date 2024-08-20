package com.github.altfatterz.postgresqlr2dbc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestPostgresqlR2dbcApplicationConfig {

    @Bean
    @ServiceConnection
        // https://java.testcontainers.org/modules/databases/postgres/
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("test").withUsername("admin").withPassword("secret");
    }

}