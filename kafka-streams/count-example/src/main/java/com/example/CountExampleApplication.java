package com.example;

import com.example.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class CountExampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(CountExampleApplication.class);

    private static final String APPLICATION_ID = "count-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:19092";
    private final static String SCHEMA_REGISTRY = "localhost:8081";

    private static final String INPUT_TOPIC = "movie-ticket-sales";
    private static final String OUTPUT_TOPIC = "movie-tickets-sold";

    public static void main(String[] args) {
        Properties config = getConfig();
        Topology topology = getTopology(ticketSaleSerde());
        KafkaStreams streams = startApp(config, topology);

        setupShutdownHook(streams);
    }

    private static SpecificAvroSerde<TicketSale> ticketSaleSerde() {
        final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, BOOTSTRAP_SERVERS);
        serde.configure(config, false);
        return serde;
    }

    private static Topology getTopology(SpecificAvroSerde<TicketSale> ticketSaleSerde) {
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), ticketSaleSerde))
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                // Set key to title and value to ticket value
                .map((k, v) -> new KeyValue<>(v.getTitle(), v.getTicketTotalValue()))
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                // Group by title
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                // Apply COUNT method
                .count()
                // Write to stream specified by outputTopic
                .toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }

    public static Properties getConfig() {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        return props;
    }

    private static KafkaStreams startApp(Properties config, Topology topology) {
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();
        return streams;
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.printf("### Stopping %s Application ###%n", APPLICATION_ID);
            streams.close();
        }));
    }
}
