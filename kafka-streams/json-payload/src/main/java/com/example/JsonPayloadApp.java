package com.example;


import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JsonPayloadApp {

    private static final Logger logger = LoggerFactory.getLogger(JsonPayloadApp.class);

    private final static String APPLICATION_ID = "json-payload-example";
    private final static String BOOTSTRAP_SERVERS = "localhost:19092";

    private final static String INPUT_TOPIC = "temperatures-topic";
    private final static String OUTPUT_TOPIC = "high-temperatures-topic";


    public static void main(String[] args) {
        Properties config = getConfig();
        Topology topology = getTopology();
        logger.info(topology.describe().toString()); // https://zz85.github.io/kafka-streams-viz/
        KafkaStreams streams = startApp(config, topology);
        setupShutdownHook(streams);
    }

    private static Serde<TemperatureReading> getTemperatureReadingSerde() {
        Map<String, Object> serdeProps = new HashMap<>();
        serdeProps.put("json.value.type", TemperatureReading.class);

        final Serializer<TemperatureReading> temperatureSerializer = new KafkaJsonSerializer<>();
        temperatureSerializer.configure(serdeProps, false);

        final Deserializer<TemperatureReading> temperatureDeserializer = new KafkaJsonDeserializer<>();
        temperatureDeserializer.configure(serdeProps, false);

        return Serdes.serdeFrom(temperatureSerializer, temperatureDeserializer);
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

    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        final Serde<TemperatureReading> temperatureReadingSerde = getTemperatureReadingSerde();

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), temperatureReadingSerde))
                .peek((key, value) -> logger.info("input with [key: {}, value: {}]", key, value))
                .filter((key, value) -> value.temperature > 25)
                .peek((key, value) -> logger.info("filtered: [key: {}, value: {}]", key, value))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), temperatureReadingSerde));

        return builder.build();
    }

    static public class TemperatureReading {
        public String station;
        public Double temperature;
        public Long timestamp;

        @Override
        public String toString() {
            return "[station: " + station + ",temperature:" + temperature + ",timestamp:" + timestamp + "]";
        }
    }

    private static void setupShutdownHook(KafkaStreams streams) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("### Stopping {} application ###", APPLICATION_ID);
            streams.close();
        }));
    }

}
