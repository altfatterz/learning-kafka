package com.example;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KafkaStreamsGettingStartedApp {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsGettingStartedApp.class);

    public static void main(String[] args) {

        logger.info("Kafka Streams Rocks!");

        Properties settings = new Properties();

        settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-getting-started");
        settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");

        final Serde<String> stringSerde = Serdes.String();
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> lines = builder.stream("lines-topic", Consumed.with(stringSerde, stringSerde));
        KStream<String, String> transformed = lines.mapValues(value -> value.toLowerCase());
        transformed.to("lines-lower-topic", Produced.with(stringSerde, stringSerde));
        Topology topology = builder.build();

        logger.info(topology.describe().toString());

        // Create the KafkaStreams app
        KafkaStreams streams = new KafkaStreams(topology, settings);

        // Add a shutdown hook for graceful termination and start the app
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down KafkaStreams instance...");
            streams.close();
            latch.countDown();
        }));

        try{
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
