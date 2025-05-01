package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class StatelessStreamProcessingExample {

    private static final Logger logger = LoggerFactory.getLogger(StatelessStreamProcessingExample.class);

    private final static String APPLICATION_ID = "stateless-kafka-streams-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:9092";

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

        // nothing is stored since the processing is stateless
        settings.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");

        // check: kafka-consumer-groups --bootstrap-server broker:9092 --group stateless-kafka-streams-example --describe
        settings.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Kafka Streams works on unbounded sequence of events called events streams (sequence of records (key value pairs))

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .peek((key, value) -> logger.info("Incoming record - key:{} value:{}", key, value))
                // mapValues useful for example credit card data and want to scrub out the first five characters
                // when you can use mapValues you should prefer it, instead of map(), reason for this is the `repartitioning`
                .mapValues(value -> value.toUpperCase().replaceAll("\\s+", " "))
                // keep only those which value is longer than 5 characters
                .filter((key, value) -> value.length() > 5)
                // there is no repartition topic ???
                .map((key, value) -> new KeyValue<>(value, value))
                .peek((key, value) -> logger.info("Outgoing record - key:{} value:{}", key, value))
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
