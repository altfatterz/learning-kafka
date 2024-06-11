package com.example.springkafkatesting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@SpringBootTest
@EmbeddedKafka(partitions = 1, kraft = true)
@ActiveProfiles("test")
class EmbeddedKafkaIntegrationTest {

    @Autowired
    private KafkaConsumer consumer;

    @Autowired
    private KafkaProducer producer;

    @Value("${topic}")
    private String topic;

    @Autowired
    // the embedded broker is cached in test application context
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void sendAndReceive() throws Exception {
        String fact = "Chuck Norris can spawn threads that complete before they are started.";
        producer.sendFact(topic, fact);
        consumer.getLatch().await(10000, TimeUnit.MILLISECONDS);

        assertThat(consumer.getLatch().getCount(), equalTo(0L));
        assertThat(consumer.getPayload(), equalTo(fact));

        System.out.println("broker URL=" + embeddedKafkaBroker.getBrokersAsString());
    }
}