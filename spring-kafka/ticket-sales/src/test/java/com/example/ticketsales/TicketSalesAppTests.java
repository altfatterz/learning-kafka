package com.example.ticketsales;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@EmbeddedKafka
@SpringBootTest
@ActiveProfiles("test")
public class TicketSalesAppTests {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void contextLoads() {
        System.out.println("Zookeeper connection: " + embeddedKafkaBroker.getZookeeperConnectionString());
        System.out.println("Embedded broker: " + embeddedKafkaBroker.getBrokersAsString());
    }

}
