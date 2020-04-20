package com.example.ticketsales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableConfigurationProperties
@EnableKafkaStreams
public class TicketSalesApp {

    public static void main(String[] args) {
        SpringApplication.run(TicketSalesApp.class, args);
    }

}
