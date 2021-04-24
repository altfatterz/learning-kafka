package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

public class StatefulStreamProcessingExample {

    private static final Logger logger = LoggerFactory.getLogger(StatefulStreamProcessingExample.class);

    private final static String APPLICATION_ID = "stateful-kafka-streams-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:19092";

    private final static String INPUT_TOPIC = "sentences-topic";
    private final static String OUTPUT_TOPIC = "word-counts-topic";

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

        // if no consumer offsets found start from the beginning
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Specify default (de)serializers for record keys and for record values.
        settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());
        settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // disable caching https://docs.confluent.io/platform/current/streams/developer-guide/memory-mgmt.html#record-caches-in-the-dsl
        settings.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // we are consuming with the default serdes
        KStream<byte[], String> sentences = builder.stream(INPUT_TOPIC);

        sentences
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                .groupBy((key, value) -> value, Grouped.keySerde(Serdes.String()))
                .count(Materialized.as("WordCount"))
                .toStream()
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping {} application ###", APPLICATION_ID);
            streams.close();
        }));
    }

}
