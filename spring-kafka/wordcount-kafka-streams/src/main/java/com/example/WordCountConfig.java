package com.example;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConfigurationProperties("topics")
public class WordCountConfig {

    private Topic input;
    private Topic output;

    private static final Logger logger = LoggerFactory.getLogger(WordCountConfig.class);

    // Spring Boot Apache Kafka Support
    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-kafka

    @Bean
    public NewTopic inputTopic() {
        return TopicBuilder.name(input.getName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic outputTopic() {
        return TopicBuilder.name(output.getName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    public Topic getInput() {
        return input;
    }

    public void setInput(Topic input) {
        this.input = input;
    }

    public Topic getOutput() {
        return output;
    }

    public void setOutput(Topic output) {
        this.output = output;
    }

    static class Topic {

        private String name;
        private int partitions;
        private int replicas;

        public Topic(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public int getReplicas() {
            return replicas;
        }

        public void setReplicas(int replicas) {
            this.replicas = replicas;
        }
    }
}