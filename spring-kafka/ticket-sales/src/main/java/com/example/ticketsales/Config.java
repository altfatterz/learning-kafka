package com.example.ticketsales;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class Config {

    public static final String INPUT_TOPIC = "movie-ticket-sales";
    public static final String OUTPUT_TOPIC = "movie-tickets-sold";

    // Spring Boot Apache Kafka Support
    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-kafka

    @Bean
    public NewTopic inputTopic() {
        return TopicBuilder.name(INPUT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic outputTopic() {
        return TopicBuilder.name(OUTPUT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}