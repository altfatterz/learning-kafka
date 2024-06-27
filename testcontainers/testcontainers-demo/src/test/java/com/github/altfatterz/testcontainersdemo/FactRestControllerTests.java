package com.github.altfatterz.testcontainersdemo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FactRestControllerTests {

    private static Logger logger = LoggerFactory.getLogger(FactRepositoryTests.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort
    int port;

    @Autowired
    FactRepository factRepository;

    @Autowired
    FactRestController factRestController;

    @Test
    void getFacts() {
        // arrange
        factRepository.deleteAll();
        factRepository.save(new Fact("Chuck Norris can dribble a bowling ball."));

        // act
        List<Fact> facts = factRestController.getFacts();

        // assert
        assertThat(facts.size()).isEqualTo(1);
        assertThat(facts.get(0).getValue()).isEqualTo("Chuck Norris can dribble a bowling ball.");
    }


    @Test
    void addFacts() {
        // arrange
        factRepository.deleteAll();

        // act
        RestClient.create().post().uri("http://localhost:" + port + "/facts")
                .body("Chuck Norris makes onions cry").retrieve();

        // assert
        List<Fact> facts = factRepository.findAll();
        assertThat(facts.size()).isEqualTo(1);
    }

    @Test
    void searchFacts() {
        // arrange
        factRepository.deleteAll();
        factRepository.save(new Fact("Chuck Norris can parallel park a train."));

        // act
        List<Fact> facts = RestClient.create().post().uri("http://localhost:" + port + "/facts/search")
                .body("chuck")
                .retrieve().body(new ParameterizedTypeReference<List<Fact>>() {
                });

        // assert
        assertThat(facts.size()).isEqualTo(1);
    }

}
