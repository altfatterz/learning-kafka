package com.example;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;


public class JoinExample {

    private static final Logger logger = LoggerFactory.getLogger(JoinExample.class);

    private static final String APPLICATION_ID = "join-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:19092";

    private static final String TOPIC1 = "left-topic";
    private static final String TOPIC2 = "right-topic";
    private static final String TOPIC3 = "joined-topic";

    public static void main(String[] args) {
        Properties config = getConfig();
        Topology topology = getTopology();
        KafkaStreams streams = startApp(config, topology);

        setupShutdownHook(streams);
    }

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        final Serde<String> stringSerde = Serdes.String();

        KStream<String, String> leftStream = builder.stream(TOPIC1, Consumed.with(stringSerde, stringSerde));
        KStream<String, String> rightStream = builder.stream(TOPIC2, Consumed.with(stringSerde, stringSerde));

        leftStream
                .join(rightStream,
                        (leftValue, rightValue) -> "[" + leftValue + ", " + rightValue + "]",
                        JoinWindows.of(Duration.ofMinutes(5)),
                        StreamJoined.with(stringSerde, stringSerde, stringSerde)
                )
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                .to(TOPIC3, Produced.with(stringSerde, stringSerde));

        return builder.build();
    }

    private static Properties getConfig() {
        Properties settings = new Properties();
        settings.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return settings;
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
