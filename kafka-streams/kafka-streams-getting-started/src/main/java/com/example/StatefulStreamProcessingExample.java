package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class StatefulStreamProcessingExample {

    private static final Logger logger = LoggerFactory.getLogger(StatefulStreamProcessingExample.class);

    private final static String APPLICATION_ID = "stateful-kafka-streams-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:29092";

    private final static String INPUT_TOPIC = "stateful-demo-input-topic";
    private final static String OUTPUT_TOPIC = "stateful-demo-output-topic";

    public static void main(String[] args) {
        Properties config = getConfig();
        Topology topology = getTopology();
        logger.info(topology.describe().toString()); // https://zz85.github.io/kafka-streams-viz/
        KafkaStreams streams = startApp(config, topology);
        setupShutdownHook(streams);
    }

    private static KafkaStreams startApp(Properties config, Topology topology) {
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();
        return streams;
    }

    private static Properties getConfig() {
        Properties settings = new Properties();
        settings.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        settings.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");

        // if no consumer offsets found start from the beginning
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Specify default (de)serializers for record keys and for record values.
        settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());
        settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // disable caching https://docs.confluent.io/platform/current/streams/developer-guide/memory-mgmt.html#record-caches-in-the-dsl
        // leave the caching for windows demonstration
        settings.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // we are consuming with the default serdes
        KStream<byte[], String> sentences = builder.stream(INPUT_TOPIC);

        sentences
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\s+")))
                .peek((key, value) -> logger.info("record after flatMapValues: [key: {}, value: {}]", key, value))
                .groupBy((key, value) -> value, Grouped.keySerde(Serdes.String()))
                //.windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
                .count(Materialized.as("WordCount"))
                .toStream()
                .peek((key, value) -> logger.info("record to be produced [key: {}, value: {}]", key, value))
//                .map((Windowed<String> key, Long count) -> new KeyValue<>(key.toString(), count))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping {} application ###", APPLICATION_ID);
            streams.close();
        }));
    }

    // TODO Enable the windowing
}
