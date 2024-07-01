package com.github.altfatterz.joinexamples;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConfigurationProperties("topics.stream-to-table")
public class StreamToTableJoinConfig {

    private Topic input1;
    private Topic input2;
    private Topic output;

    // Spring Boot Apache Kafka Support
    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-kafka

    @Bean
    public NewTopic input1() {
        return TopicBuilder.name(input1.getName())
                .partitions(input1.getPartitions())
                .replicas(input1.getReplicas())
                .build();
    }

    @Bean
    public NewTopic input2() {
        return TopicBuilder.name(input2.getName())
                .partitions(input2.getPartitions())
                .replicas(input2.getReplicas())
                .build();
    }

    @Bean
    public NewTopic output() {
        return TopicBuilder.name(output.getName())
                .partitions(output.getPartitions())
                .replicas(output.getReplicas())
                .build();
    }

    public Topic getInput1() {
        return input1;
    }

    public Topic getInput2() {
        return input2;
    }

    public Topic getOutput() {
        return output;
    }

    public void setInput1(Topic input1) {
        this.input1 = input1;
    }

    public void setInput2(Topic input2) {
        this.input2 = input2;
    }

    public void setOutput(Topic output) {
        this.output = output;
    }

    public static class Topic {

        private String name;
        private int partitions;
        private int replicas;

        public Topic(String name, int partitions, int replicas) {
            this.name = name;
            this.partitions = partitions;
            this.replicas = replicas;
        }

        public String getName() {
            return name;
        }

        public int getPartitions() {
            return partitions;
        }

        public int getReplicas() {
            return replicas;
        }

    }
}