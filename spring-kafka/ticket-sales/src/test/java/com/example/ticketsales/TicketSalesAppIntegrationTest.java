package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.util.Arrays.asList;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

// https://github.com/deepi2003/AvroSample/blob/master/src/test/java/com/deepti/kafka/sample/integrationtest/UserIntegrationTest.java
// https://github.com/spring-projects/spring-kafka/issues/2291

@EmbeddedKafka(kraft = true, partitions = 1, topics = { "${topics.input.name}", "${topics.output.name}"})
@SpringBootTest
@ActiveProfiles("test")
public class TicketSalesAppIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TicketSalesAppIntegrationTest.class);

    private static final String SCHEMA_REGISTRY_SCOPE = TicketSalesAppIntegrationTest.class.getName();
    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;

    @Value("${topics.input.name}")
    private String inputTopic;

    @Value("${topics.output.name}")
    private String outputTopic;

    @Autowired
    // the embedded broker is cached in test application context
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Producer<Object, Object> producer;
    private Consumer<String, String> consumer;

    @BeforeEach
    public void setUp() {
        initializeConsumer();
        initializeProducer();
    }

    @Test
    void brokerInfo() {
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());
        logger.info("List of topics: {}", embeddedKafkaBroker.getTopics());
    }

    @Test
    void countSales() {
        // arrange
        List<TicketSale> input = asList(
                new TicketSale("Die Hard", "2019-07-18T10:00:00Z", 12),
                new TicketSale("Die Hard", "2019-07-18T10:01:00Z", 12),
                new TicketSale("The Godfather", "2019-07-18T10:01:31Z", 12),
                new TicketSale("Die Hard", "2019-07-18T10:01:36Z", 24),
                new TicketSale("The Godfather", "2019-07-18T10:02:00Z", 18),
                new TicketSale("The Big Lebowski", "2019-07-18T11:03:21Z", 12),
                new TicketSale("The Big Lebowski", "2019-07-18T11:03:50Z", 12),
                new TicketSale("The Godfather", "2019-07-18T11:40:00Z", 36),
                new TicketSale("The Godfather", "2019-07-18T11:40:09Z", 18)
        );

        // act
        for (TicketSale ticketSale : input) {
            producer.send(new ProducerRecord<>(inputTopic, null, ticketSale));
        }

        // assert
        String suffix = " tickets sold";
        List<KeyValue<String, String>> expectedOutput = List.of(
                KeyValue.pair("Die Hard", "1" + suffix ),
                KeyValue.pair("Die Hard", "2" + suffix ),
                KeyValue.pair("The Godfather", "1" + suffix ),
                KeyValue.pair("Die Hard", "3" + suffix ),
                KeyValue.pair("The Godfather", "2" + suffix ),
                KeyValue.pair("The Big Lebowski", "1" + suffix ),
                KeyValue.pair("The Big Lebowski", "2" + suffix ),
                KeyValue.pair("The Godfather", "3" + suffix ),
                KeyValue.pair("The Godfather", "4" + suffix )
        );

        List<KeyValue<String, String>> actualOutput = new ArrayList<>();
        ConsumerRecords<String, String> output = KafkaTestUtils.getRecords(consumer);
        for (ConsumerRecord<String, String> consumerRecord : output) {
            actualOutput.add(new KeyValue<>(consumerRecord.key(), consumerRecord.value()));
        }

        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());

        SpecificAvroSerde<TicketSale> ticketSaleSerde = TestUtils.getTicketSaleSerde(MOCK_SCHEMA_REGISTRY_URL);

        producerConfigs.put(VALUE_SERIALIZER_CLASS_CONFIG, ticketSaleSerde.serializer().getClass());
        producerConfigs.put(SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        producer = new DefaultKafkaProducerFactory<>(producerConfigs).createProducer();
    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = KafkaTestUtils
                .consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.String().deserializer()).createConsumer();

        consumer.subscribe(Collections.singleton(outputTopic));
    }

}
