package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaStreamsGettingStartedApp {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsGettingStartedApp.class);

    private final static String APPLICATION_ID = "lowercase-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:19092";

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
        return settings;
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> lines = builder.stream("lines-topic", Consumed.with(Serdes.String(),
                Serdes.String()));
        KStream<String, String> transformed = lines.mapValues(value -> value.toLowerCase());
        transformed.to("lines-lower-topic", Produced.with(Serdes.String(), Serdes.String()));
        return builder.build();
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping {} application ###", APPLICATION_ID);
            streams.close();
        }));
    }

}
