package com.example;


import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainer")
public class WordCountTestcontainerIntegrationTest {

    @Container
    private static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).withKraft();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Value("${topics.input.name}")
    private String input;

    @Value("${topics.output.name}")
    private String output;

    @Autowired
    private WordCountConfig config;

    private Producer<String, String> producer;
    private Consumer<String, Long> consumer;

    @Test
    public void wordCount() {
        // arrange
        List<String> facts = asList(
                "Chuck Norris doesn't need an OS.",
                "The Swiss Army uses Chuck Norris Knives",
                "Chuck Norris has a diary, it is called the Guinness Book Of World Records"
        );

        // act
        for (String fact : facts) {
            producer.send(new ProducerRecord<>(input, null, fact));
        }

        // assert
        List<KeyValue<String, Long>> expectedOutput = List.of(
                KeyValue.pair("norris", 1L),
                KeyValue.pair("norris", 2L),
                KeyValue.pair("knives", 1L),
                KeyValue.pair("norris", 3L),
                KeyValue.pair("called", 1L),
                KeyValue.pair("guinness", 1L),
                KeyValue.pair("records", 1L)
        );

        ConsumerRecords<String, Long> records = consumer.poll(Duration.ofSeconds(10));

        List<KeyValue<String, Long>> actualOutput = new ArrayList<>();
        for (ConsumerRecord<String, Long> record : records) {
            actualOutput.add(new KeyValue<>(record.key(), record.value()));
        }

    }

    @BeforeEach
    public void setUp() {
        initializeConsumer();
        initializeProducer();
    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs = new HashMap();
        producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producer = new DefaultKafkaProducerFactory<>(producerConfigs,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();
    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = new HashMap();
        consumerConfigs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerConfigs.put(GROUP_ID_CONFIG, "test-group");
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.Long().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.Long().deserializer()).createConsumer();

        consumer.subscribe(Collections.singleton(output));
    }


}
