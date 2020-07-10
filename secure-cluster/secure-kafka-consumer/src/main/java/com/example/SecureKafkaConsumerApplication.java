package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SecureKafkaConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureKafkaConsumerApplication.class, args);
    }

}

@Component
class Consumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private static final String TOPIC = "secure-topic";

    @KafkaListener(id = "messages-container", groupId = "secure-kafka-consumer", topics = TOPIC)
    public void consume(String message) {
        logger.info("Consumed message -> {}", message);
    }

}
