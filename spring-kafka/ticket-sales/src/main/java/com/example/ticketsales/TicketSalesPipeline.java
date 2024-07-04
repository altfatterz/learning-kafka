package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

@Configuration
public class TicketSalesPipeline {

    private final KafkaProperties kafkaProperties;
    private final TicketSalesConfig ticketSalesConfig;

    @Autowired
    private Environment env;

    private static final Logger logger = LoggerFactory.getLogger(TicketSalesPipeline.class);

    public TicketSalesPipeline(KafkaProperties kafkaProperties, TicketSalesConfig topicsConfig) {
        this.kafkaProperties = kafkaProperties;
        this.ticketSalesConfig = topicsConfig;
    }

    @Bean
    public KStream<String, String> buildPipeline(StreamsBuilder streamsBuilder) {

        KStream<String, String> kStream = streamsBuilder
                .stream(ticketSalesConfig.getInput().getName(), Consumed.with(Serdes.String(), ticketSaleSerde()))
                .peek((key, value) -> logger.info("input record - key: {} value: {}", key, value))
                // Set key to title and value to ticket value
                .map((k, v) -> new KeyValue<>(v.getTitle(), v.getTicketTotalValue()))
                .peek((key, value) -> logger.info("after map - key: {} value: {}", key, value))
                // Group by title
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                // Apply COUNT method
                .count()
                // Write to stream specified by outputTopic
                .toStream()
                .mapValues(v -> v.toString() + " tickets sold")
                .peek((key, value) -> logger.info("output record - key: {} value: {}", key, value));

        kStream.to(ticketSalesConfig.getOutput().getName(), Produced.with(Serdes.String(), Serdes.String()));

        return kStream;
    }

    private SpecificAvroSerde<TicketSale> ticketSaleSerde() {
        final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();

        // This does not work with testcontainers, the @DynamicPropertySource is
        // not updating the KafkaProperties only the Environment
        String schemaRegistryURL = kafkaProperties.getStreams().getProperties().get(SCHEMA_REGISTRY_URL_CONFIG);

        // String schemaRegistryURL = env.getProperty("spring.kafka.streams.properties[0].schema.registry.url");

        logger.info("setting the ticketSaleSerde - schemaRegistryURL:" + schemaRegistryURL);
        config.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);
        serde.configure(config, false);
        return serde;
    }
}
