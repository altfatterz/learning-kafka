package com.github.altfatterz.joinexamples;

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
import java.time.Instant;
import java.util.*;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(kraft = true, partitions = 1,
        topics = {"${topics.stream-to-stream.input1}",
                "${topics.stream-to-stream.input2}",
                "${topics.stream-to-stream.output}"})
@SpringBootTest
@ActiveProfiles("test")
public class StreamToStreamJoinPipelineIntegrationTest {
    private static Logger logger = LoggerFactory.getLogger(StreamToStreamJoinPipelineIntegrationTest.class);

    @Value("${topics.stream-to-stream.input1}")
    private String adImpressionsTopic;

    @Value("${topics.stream-to-stream.input2}")
    private String adClicksTopic;

    @Value("${topics.stream-to-stream.output}")
    private String adImpressionsAndClicksTopic;

    @Autowired
    // the embedded broker is cached in test application context
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private StreamToStreamJoinConfig config;

    private Producer<String, String> adImpressionsProducer;
    private Producer<String, String> adClicksProducer;
    private Consumer<String, String> adImpressionsAndClicksConsumer;


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
        Map<String, Object> producerConfigs1 = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs1.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs1.put(VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        adImpressionsProducer = new DefaultKafkaProducerFactory<>(producerConfigs1,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();

        Map<String, Object> producerConfigs2 = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs2.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs2.put(VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        adClicksProducer = new DefaultKafkaProducerFactory<>(producerConfigs2,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();

    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = KafkaTestUtils
                .consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        adImpressionsAndClicksConsumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.String().deserializer()).createConsumer();

        adImpressionsAndClicksConsumer.subscribe(Collections.singleton(adImpressionsAndClicksTopic));
    }
}
