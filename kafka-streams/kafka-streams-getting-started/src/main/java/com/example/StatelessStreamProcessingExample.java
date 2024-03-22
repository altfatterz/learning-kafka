package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class StatelessStreamProcessingExample {

    private static final Logger logger = LoggerFactory.getLogger(StatelessStreamProcessingExample.class);

    private final static String APPLICATION_ID = "stateless-kafka-streams-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:29092";

    private final static String INPUT_TOPIC = "stateless-demo-input-topic";
    private final static String OUTPUT_TOPIC = "stateless-demo-output-topic";

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

        // check the number of tasks created
        // [0_0], [0_1], [0_2]

        // Start up another instance and see how the stream tasks are balanced
        // Check the:  kafka-consumer-groups --bootstrap-server kafka:9092 --group stateless-kafka-streams-example --describe

        // Stop the second instance modify this line and see the consumer group
        // settings.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Kafka Streams works on unbounded sequence of events called events streams (sequence of records (key value pairs))

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .peek((key, value) -> logger.info("Incoming record - key:" + key + " value:" + value))
                // mapValues useful for example credit card data and want to scrub out the first five characters
                // when you can use mapValues you should prefer it, instead of map(), reason for this is the `repartitioning`
                .mapValues(value -> value.toLowerCase())
                // keep only those which value is longer than 5 characters
                .filter((key, value) -> value.length() > 5)
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
