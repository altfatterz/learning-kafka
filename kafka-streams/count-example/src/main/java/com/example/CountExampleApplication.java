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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class CountExampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(CountExampleApplication.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String APPLICATION_ID = props.getProperty("applicationId");
        final String SCHEMA_REGISTRY_URL = props.getProperty("schema.registry.url");
        final String INPUT_TOPIC = props.getProperty("input-topic");
        final String OUTPUT_TOPIC = props.getProperty("output-topic");


        Topology topology = getTopology(INPUT_TOPIC, OUTPUT_TOPIC, ticketSaleSerde(SCHEMA_REGISTRY_URL));
        KafkaStreams streams = startApp(props, topology);

        setupShutdownHook(APPLICATION_ID, streams);
    }

    private static SpecificAvroSerde<TicketSale> ticketSaleSerde(String schemaRegistryURL) {
        final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);
        serde.configure(config, false);
        return serde;
    }

    private static Topology getTopology(String inputTopic, String outputTopic, SpecificAvroSerde<TicketSale> ticketSaleSerde) {
        StreamsBuilder builder = new StreamsBuilder();

        // two new topics were created and also two new folders (state stores) under /tmp/kafka-streams/count-example
        // 0_0 and 0_1

        // initially the keys are null, but we still need to set the SerDe for keys
        builder.stream(inputTopic, Consumed.with(Serdes.String(), ticketSaleSerde))
                .peek((key, value) -> logger.info("received record with key: {}, value: {}", key, value))
                // Set key to title and value to ticket value
                // creates a repartition topic since we change the key
                .map((k, v) -> new KeyValue<>(v.getTitle(), v.getTicketTotalValue()))
                .peek((key, value) -> logger.info("after mapping the record is with key: {}, value: {}", key, value))
                // group by title
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                // apply COUNT method, creates the changelog topic
                .count()
                // since we cannot write a Ktable to a topic we need to convert it to a stream first
                .toStream()
                .peek((key, value) -> logger.info("final result with key: {}, value: {}", key, value))
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }



    private static KafkaStreams startApp(Properties config, Topology topology) {
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();
        return streams;
    }

    private static void setupShutdownHook(String applicationId, KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping %s Application ###%n", applicationId);
            streams.close();
        }));
    }
}
