package com.github.altfatterz.joinexamples;

import org.apache.commons.collections.map.HashedMap;
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
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

// https://maciejwalkowiak.com/blog/testcontainers-spring-boot-setup/

@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainer")
public class StreamToStreamTestcontainerIntegrationTest {

    @Container
    private static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).withKraft();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private StreamToStreamJoinConfig config;
    private Producer<String, String> adImpressionsProducer;
    private Producer<String, String> adClicksProducer;
    private Consumer<String, String> adImpressionsAndClicksConsumer;

    @Value("${topics.stream-to-stream.input1}")
    private String adImpressionsTopic;

    @Value("${topics.stream-to-stream.input2}")
    private String adClicksTopic;

    @Value("${topics.stream-to-stream.output}")
    private String adImpressionsAndClicksTopic;

    @BeforeEach
    public void setUp() {
        initializeConsumer();
        initializeProducer();
    }

    @Test
    void adImpressionsAndClicks() {
        // arrange
        Instant now = Instant.now();

        adImpressionsProducer.send(new ProducerRecord<>(adImpressionsTopic, "car-advertisement", "shown"));

        adImpressionsProducer.send(new ProducerRecord<>(adImpressionsTopic, null,
                now.plusSeconds(config.getWindowSizeInSeconds() + 2).toEpochMilli(),
                "newspaper-advertisement", "shown"));

        adClicksProducer.send(new ProducerRecord<>(adClicksTopic, null,
                now.plusSeconds(config.getWindowSizeInSeconds() + 3).toEpochMilli(),
                "newspaper-advertisement", "clicked"));

        // assert
        List<KeyValue<String, String>> expectedOutput = Arrays.asList(
                new KeyValue<>("car-advertisement", "shown/not-clicked-yet"),
                new KeyValue<>("newspaper-advertisement", "shown/clicked")
        );

        ConsumerRecords<String, String> output = KafkaTestUtils.getRecords(adImpressionsAndClicksConsumer,
                Duration.ofSeconds(10));

        List<KeyValue<String, String>> actualOutput = new ArrayList<>();
        for (ConsumerRecord<String, String> consumerRecord : output) {
            actualOutput.add(new KeyValue<>(consumerRecord.key(), consumerRecord.value()));
        }

        assertThat(actualOutput).isEqualTo(expectedOutput);

    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs1 = new HashedMap();
        producerConfigs1.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerConfigs1.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs1.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        adImpressionsProducer = new DefaultKafkaProducerFactory<>(producerConfigs1,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();

        Map<String, Object> producerConfigs2 = new HashedMap();
        producerConfigs2.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerConfigs2.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs2.put(VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        adClicksProducer = new DefaultKafkaProducerFactory<>(producerConfigs2,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();

    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = new HashedMap();
        consumerConfigs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerConfigs.put(GROUP_ID_CONFIG, "test-group");
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        adImpressionsAndClicksConsumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.String().deserializer()).createConsumer();

        adImpressionsAndClicksConsumer.subscribe(Collections.singleton(adImpressionsAndClicksTopic));
    }
}
