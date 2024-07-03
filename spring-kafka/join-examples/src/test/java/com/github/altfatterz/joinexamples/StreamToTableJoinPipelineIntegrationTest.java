package com.github.altfatterz.joinexamples;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(kraft = true, partitions = 1,
        topics = {"${topics.stream-to-table.input1}",
                "${topics.stream-to-table.input2}",
                "${topics.stream-to-table.output}"})
@SpringBootTest
@ActiveProfiles("test")
public class StreamToTableJoinPipelineIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(StreamToTableJoinPipelineIntegrationTest.class);

    @Value("${topics.stream-to-table.input1}")
    private String userClicksTopic;

    @Value("${topics.stream-to-table.input2}")
    private String userRegionsTopic;

    @Value("${topics.stream-to-table.output}")
    private String outputTopic;

    @Autowired
    // the embedded broker is cached in test application context
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Producer<String, Long> userClicksProducer;
    private Producer<String, String> userRegionsProducer;
    private Consumer<String, Long> clicksPerRegionConsumer;


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
    void shouldCountClicksPerRegion() {
        // arrange
        List<KeyValue<String, String>> userRegions = Arrays.asList(
                new KeyValue<>("alice", "asia"),   /* Alice lived in Asia originally... */
                new KeyValue<>("bob", "americas"),
                new KeyValue<>("chao", "asia"),
                new KeyValue<>("dave", "europe"),
                new KeyValue<>("alice", "europe"), /* ...but moved to Europe some time later. */
                new KeyValue<>("eve", "americas"),
                new KeyValue<>("fang", "asia")
        );
        for (KeyValue<String, String> userRegion : userRegions) {
            userRegionsProducer.send(new ProducerRecord<>(userRegionsTopic, userRegion.key, userRegion.value));
        }

        List<KeyValue<String, Long>> userClicks = Arrays.asList(
                new KeyValue<>("alice", 13L),
                new KeyValue<>("bob", 4L),
                new KeyValue<>("chao", 25L),
                new KeyValue<>("bob", 19L),
                new KeyValue<>("dave", 56L),
                new KeyValue<>("eve", 78L),
                new KeyValue<>("alice", 40L),
                new KeyValue<>("fang", 99L)
        );
        for (KeyValue<String, Long> userClick : userClicks) {
            userClicksProducer.send(new ProducerRecord<>(userClicksTopic, userClick.key, userClick.value));
        }

        // assert
        List<KeyValue<String, Long>> expectedOutput = Arrays.asList(
                new KeyValue<>("europe", 13L),
                new KeyValue<>("americas", 4L),
                new KeyValue<>("asia", 25L),
                new KeyValue<>("americas", 23L),
                new KeyValue<>("europe", 69L),
                new KeyValue<>("americas", 101L),
                new KeyValue<>("europe", 109L),
                new KeyValue<>("asia", 124L)
        );

        List<KeyValue<String, Long>> actualOutput = new ArrayList<>();
        ConsumerRecords<String, Long> output = KafkaTestUtils.getRecords(clicksPerRegionConsumer);
        for (ConsumerRecord<String, Long> consumerRecord : output) {
            actualOutput.add(new KeyValue<>(consumerRecord.key(), consumerRecord.value()));
        }

        assertThat(actualOutput).isEqualTo(expectedOutput);

    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs1 = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs1.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs1.put(VALUE_SERIALIZER_CLASS_CONFIG, Serdes.Long().serializer().getClass());
        userClicksProducer = new DefaultKafkaProducerFactory<>(producerConfigs1,
                Serdes.String().serializer(), Serdes.Long().serializer()).createProducer();

        Map<String, Object> producerConfigs2 = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfigs2.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        producerConfigs2.put(VALUE_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
        userRegionsProducer = new DefaultKafkaProducerFactory<>(producerConfigs2,
                Serdes.String().serializer(), Serdes.String().serializer()).createProducer();

    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = KafkaTestUtils
                .consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.Long().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        clicksPerRegionConsumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.Long().deserializer()).createConsumer();

        clicksPerRegionConsumer.subscribe(Collections.singleton(outputTopic));
    }

}