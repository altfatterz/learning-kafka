package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;


/**
 * When the embedded Kafka and embedded Zookeeper server are started by the EmbeddedKafkaBroker,
 * a system property named `spring.embedded.kafka.brokers` is set to the address of the Kafka brokers and
 * a system property named `spring.embedded.zookeeper.connect` is set to the address of Zookeeper
 *
 * @EmbeddedKafka registers the EmbeddedKafkaBroker bean.
 */


@EmbeddedKafka(kraft = true, topics = { "topic1", "topic2"} )
@SpringBootTest
@ActiveProfiles("test")
public class TicketSalesAppIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TicketSalesAppIntegrationTest.class);

    @Autowired
    // the embedded broker is cached in test application context
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void test1() {
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());
        logger.info("List of topics: {}", embeddedKafkaBroker.getTopics());
        kafkaTemplate.send("dummy", "hello");
    }

    @Test
    void test2() {
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());
        logger.info("List of topics: {}", embeddedKafkaBroker.getTopics());
        kafkaTemplate.send("dummy", "hello");
    }




}
