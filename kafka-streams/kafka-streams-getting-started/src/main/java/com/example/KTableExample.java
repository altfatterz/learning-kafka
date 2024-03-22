package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KTableExample {

    private static final Logger logger = LoggerFactory.getLogger(StatelessStreamProcessingExample.class);

    private final static String APPLICATION_ID = "ktable-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:29092";

    private final static String INPUT_TOPIC = "ktable-input-topic";
    private final static String OUTPUT_TOPIC = "ktable-output-topic";

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

        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Kafka Streams works on unbounded sequence of events called events streams (sequence of records (key value pairs))

        KTable<String, String> kTable = builder.table(INPUT_TOPIC,
                Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("ktable-store")
                        .withKeySerde(Serdes.String()).withValueSerde(Serdes.String()));

        String orderNumberPrefix = "orderNumber-";

        kTable.filter(((key, value) -> value.contains(orderNumberPrefix)))
                .mapValues(value -> value.substring(value.indexOf("-") + 1))
                .filter((key, value) -> Long.parseLong(value) > 1000)
                .toStream()
                .peek((key, value) -> logger.info("Outgoing record - key:" + key + " value:" + value))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping {} application ###", APPLICATION_ID);
            streams.close();
        }));
    }
}
