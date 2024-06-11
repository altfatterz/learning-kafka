package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
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

import java.util.Collections;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

// https://github.com/deepi2003/AvroSample/blob/master/src/test/java/com/deepti/kafka/sample/integrationtest/UserIntegrationTest.java

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
    void test1() {
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());
        logger.info("List of topics: {}", embeddedKafkaBroker.getTopics());

        TicketSale ticketSale = new TicketSale("Die Hard", "2019-07-18T10:00:00Z", 12);

        producer.send(new ProducerRecord<>(inputTopic, null, ticketSale));
        producer.flush();

        ConsumerRecord<String, String> output = KafkaTestUtils.getSingleRecord(consumer, outputTopic);
        System.out.println(output.key() + ":" + output.value());
    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());

        Map<String, String> config = Map.of(SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        SpecificAvroSerde<TicketSale> ticketSaleSerde = new SpecificAvroSerde<>();
        ticketSaleSerde.configure(config, false);

        producerConfigs.put(VALUE_SERIALIZER_CLASS_CONFIG, ticketSaleSerde.serializer().getClass());
        producerConfigs.put(SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        producer = new DefaultKafkaProducerFactory<>(producerConfigs).createProducer();
    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = KafkaTestUtils
                .consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.String().deserializer()).createConsumer();

        consumer.subscribe(Collections.singleton(outputTopic));
    }

    @Test
    void test2() {
        logger.info("Embedded broker connection: {}", embeddedKafkaBroker.getBrokersAsString());
        logger.info("List of topics: {}", embeddedKafkaBroker.getTopics());
//        kafkaTemplate.send("dummy", "hello");
    }


}
