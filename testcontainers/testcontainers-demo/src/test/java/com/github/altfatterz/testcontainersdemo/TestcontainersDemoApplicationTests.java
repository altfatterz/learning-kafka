package com.github.altfatterz.testcontainersdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import(TestcontainersDemoApplicationConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TestcontainersDemoApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private FactRestController factRestController;

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
        RestClient restClient = RestClient.create();
        restClient.post().uri("http://localhost:" + port + "/facts")
                .body("Chuck Norris makes onions cry").retrieve();

        // assert
        List<Fact> facts = factRepository.findAll();
        assertThat(facts.size()).isEqualTo(1);
    }

    @Test
    void test2() {
        List<Fact> facts = factRestController.getFacts();
        System.out.println(facts);
    }
}
