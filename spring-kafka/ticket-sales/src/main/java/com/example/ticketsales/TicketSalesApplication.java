package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableKafkaStreams
public class TicketSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketSalesApplication.class, args);
    }

    @Bean
    public KStream<String, Long> kStream(StreamsBuilder streamsBuilder) {

        KStream<String, Long> kStream = streamsBuilder.stream(Config.INPUT_TOPIC, Consumed.with(Serdes.String(), ticketSaleSerde()))
                // Set key to title and value to ticket value
                .map((k, v) -> new KeyValue<>((String) v.getTitle(), (Integer) v.getTicketTotalValue()))
                // Group by title
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                // Apply COUNT method
                .count()
                // Write to stream specified by outputTopic
                .toStream();

        kStream.to(Config.OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return kStream;
    }

    private SpecificAvroSerde<TicketSale> ticketSaleSerde() {
        final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();
        // TODO externalise
        config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        serde.configure(config, false);
        return serde;
    }


}
