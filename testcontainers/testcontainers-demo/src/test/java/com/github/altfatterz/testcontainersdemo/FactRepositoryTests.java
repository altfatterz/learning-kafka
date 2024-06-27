package com.github.altfatterz.testcontainersdemo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FactRepositoryTests {

    private static Logger logger = LoggerFactory.getLogger(FactRepositoryTests.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    FactRepository factRepository;

    @Autowired
    JdbcConnectionDetails jdbcConnectionDetails;

    @Test
    void connectionEstablished() {
        logger.info("driver: " + jdbcConnectionDetails.getDriverClassName());
        logger.info("jdbc url: " + jdbcConnectionDetails.getJdbcUrl());
        logger.info("username: " + jdbcConnectionDetails.getUsername());
        logger.info("password: " + jdbcConnectionDetails.getPassword());
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void searchFactsFound() {
        // arrange
        factRepository.deleteAll();
        factRepository.save(new Fact("Chuck Norris can dribble a bowling ball."));

        // act
        List<Fact> facts = factRepository.findFactsByValueContainingIgnoreCase("chuck");

        // assert
        assertThat(facts.size()).isEqualTo(1);
    }

    @Test
    void searchFactsNotFound() {
        // arrange
        factRepository.deleteAll();
        factRepository.save(new Fact("Chuck Norris can dribble a bowling ball."));

        // act
        List<Fact> facts = factRepository.findFactsByValueContainingIgnoreCase("chuk");

        // assert
        assertThat(facts.size()).isEqualTo(0);
    }

}
