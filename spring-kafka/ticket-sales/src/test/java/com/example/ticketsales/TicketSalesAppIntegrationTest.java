package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;


/**
 * When the embedded Kafka and embedded Zookeeper server are started by the EmbeddedKafkaBroker,
 * a system property named `spring.embedded.kafka.brokers` is set to the address of the Kafka brokers and
 * a system property named `spring.embedded.zookeeper.connect` is set to the address of Zookeeper
 *
 * @EmbeddedKafka registers the EmbeddedKafkaBroker bean.
 */
@EmbeddedKafka
@SpringBootTest
@ActiveProfiles("test")
public class TicketSalesAppIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TicketSalesAppIntegrationTest.class);

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
        logger.info("Zookeeper connection: {}", embeddedKafkaBroker.getZookeeperConnectionString());
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());

        TicketSale ticketSale = new TicketSale("Die Hard", "2019-07-18T10:00:00Z", 12);

        kafkaTemplate.send("movie-ticket-sales", "hello");
    }

}
