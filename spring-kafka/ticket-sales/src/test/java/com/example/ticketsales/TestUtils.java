package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class TestUtils {

    public static SpecificAvroSerde<TicketSale> getTicketSaleSerde(String schemaRegistryURL) {
        Map<String, String> config = Map.of(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);
        SpecificAvroSerde<TicketSale> ticketSaleSerde = new SpecificAvroSerde<>();
        ticketSaleSerde.configure(config, false);
        return ticketSaleSerde;
    }
}
