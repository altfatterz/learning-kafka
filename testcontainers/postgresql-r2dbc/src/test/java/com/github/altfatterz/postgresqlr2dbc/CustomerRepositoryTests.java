package com.github.altfatterz.postgresqlr2dbc;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataR2dbcTest
@Testcontainers
class CustomerRepositoryTests {

    private static Logger logger = LoggerFactory.getLogger(CustomerRepositoryTests.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    R2dbcConnectionDetails r2dbcConnectionDetails;

    @Test
    void connectionEstablished() {
        logger.info("r2dbcConnectionDetails: " + r2dbcConnectionDetails.getConnectionFactoryOptions().toString());
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void executesFindByLastname() {
        var john = new Customer(null, "John", "Doe");
        var jane = new Customer(null, "Jane", "Doe");

        // act and verify
        customerRepository.findByLastname("Doe")
                .as(StepVerifier::create)
                .expectNext(john)
                .expectNext(jane)
                .verifyComplete();
    }

}
