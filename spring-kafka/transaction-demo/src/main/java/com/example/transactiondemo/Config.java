package com.example.transactiondemo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class Config {

    public static final String WORDS = "words";

    public static final String UPPERCASE_WORDS = "uppercase_words";

    @Bean
    public NewTopic words() {
        return TopicBuilder.name(WORDS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic uppercaseWords() {
        return TopicBuilder.name(UPPERCASE_WORDS).partitions(1).replicas(1).build();
    }

}
