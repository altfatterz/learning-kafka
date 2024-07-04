package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.util.Arrays.asList;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainer")
public class TicketSalesTestcontainerIntegrationTest {

    private static final Network NETWORK = Network.newNetwork();

    @Container
    private static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withKraft()
            .withNetwork(NETWORK);

    @Container
    private static GenericContainer schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.6.1"))
            .dependsOn(kafkaContainer)
            .withNetwork(NETWORK)
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                    "PLAINTEXT://" + kafkaContainer.getNetworkAliases().get(0) + ":9092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.streams.properties[0].schema.registry.url",
                new Supplier<>() {
                    @Override
                    public Object get() {
                        return "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
                    }
                });
    }

    private static Logger logger = LoggerFactory.getLogger(TicketSalesTestcontainerIntegrationTest.class);

    @Value("${topics.input.name}")
    private String inputTopic;

    @Value("${topics.output.name}")
    private String outputTopic;

    private Producer<String, TicketSale> producer;
    private Consumer<String, String> consumer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Test
    public void test() {
        // arrange
        List<TicketSale> ticketSales = asList(
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
        for (TicketSale ticketSale : ticketSales) {
            producer.send(new ProducerRecord<>(inputTopic, ticketSale));
        }

        String suffix = " tickets sold";

        // assert
        List<KeyValue<String, String>> expectedOutput = List.of(
                KeyValue.pair("Die Hard", "1" + suffix),
                KeyValue.pair("Die Hard", "2" + suffix),
                KeyValue.pair("The Godfather", "1" + suffix),
                KeyValue.pair("Die Hard", "3" + suffix),
                KeyValue.pair("The Godfather", "2" + suffix),
                KeyValue.pair("The Big Lebowski", "1" + suffix),
                KeyValue.pair("The Big Lebowski", "2" + suffix),
                KeyValue.pair("The Godfather", "3" + suffix),
                KeyValue.pair("The Godfather", "4" + suffix)
        );

        List<KeyValue<String, String>> actualOutput = new ArrayList<>();
        ConsumerRecords<String, String> output = KafkaTestUtils.getRecords(consumer);
        for (ConsumerRecord<String, String> consumerRecord : output) {
            actualOutput.add(new KeyValue<>(consumerRecord.key(), consumerRecord.value()));
        }

        assertThat(actualOutput).isEqualTo(expectedOutput);

    }

    @BeforeEach
    public void setUp() {
        initializeConsumer();
        initializeProducer();
    }

    private void initializeProducer() {
        Map<String, Object> producerConfigs = new HashMap<>();
        producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerConfigs.put(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());

        String schemaRegistryURL = "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
        System.out.println("schemaRegistryURL:" + schemaRegistryURL);
        SpecificAvroSerde<TicketSale> ticketSaleSerde = TestUtils.getTicketSaleSerde(schemaRegistryURL);

        producerConfigs.put(VALUE_SERIALIZER_CLASS_CONFIG, ticketSaleSerde.serializer().getClass());
        producerConfigs.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);

        producer = new DefaultKafkaProducerFactory<String, TicketSale>(producerConfigs).createProducer();
    }

    private void initializeConsumer() {
        Map<String, Object> consumerConfigs = new HashMap<>();
        consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerConfigs.put(KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(VALUE_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
        consumerConfigs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerConfigs,
                Serdes.String().deserializer(), Serdes.String().deserializer()).createConsumer();

        consumer.subscribe(Collections.singleton(outputTopic));
    }

}
