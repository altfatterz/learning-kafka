package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

@Configuration
public class TicketSalesStreamConfig {

    private final KafkaProperties kafkaProperties;
    private final TopicsConfig topicsConfig;

    public TicketSalesStreamConfig(KafkaProperties kafkaProperties, TopicsConfig topicsConfig) {
        this.kafkaProperties = kafkaProperties;
        this.topicsConfig = topicsConfig;
    }

    @Bean
    public KStream<String, Long> kStream(StreamsBuilder streamsBuilder) {

        KStream<String, Long> kStream = streamsBuilder
                .stream(topicsConfig.input.getName(), Consumed.with(Serdes.String(), ticketSaleSerde()))
                // Set key to title and value to ticket value
                .map((k, v) -> new KeyValue<>(v.getTitle(), v.getTicketTotalValue()))
                // Group by title
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                // Apply COUNT method
                .count()
                // Write to stream specified by outputTopic
                .toStream();

        kStream.to(topicsConfig.output.getName(), Produced.with(Serdes.String(), Serdes.Long()));

        return kStream;
    }

    private SpecificAvroSerde<TicketSale> ticketSaleSerde() {
        final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();
        String schemaRegistryURL = kafkaProperties.getStreams().getProperties().get(SCHEMA_REGISTRY_URL_CONFIG);
        config.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);
        serde.configure(config, false);
        return serde;
    }
}
