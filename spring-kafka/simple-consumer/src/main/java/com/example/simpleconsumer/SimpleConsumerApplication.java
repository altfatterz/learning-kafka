package com.example.simpleconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SimpleConsumerApplication {


    // The listener containers created for @KafkaListener annotations are not beans in the application context.
    // Instead, they are registered with an infrastructure bean of type KafkaListenerEndpointRegistry
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    private static final Logger logger = LoggerFactory.getLogger(SimpleConsumerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SimpleConsumerApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> registry.getAllListenerContainers().forEach(messageListenerContainer ->
                logger.info("{}:{}",
                        messageListenerContainer.getListenerId(),
                        messageListenerContainer.getGroupId()));
    }

}

@Component
class Consumer {

    private final Logger logger = LoggerFactory.getLogger(Consumer.class);

    @KafkaListener(id = "messages-container", groupId = "messages-group", topics = Config.TOPIC)
    public void consume(String message) {
        if (message.startsWith("fail")) {
            throw new RuntimeException("failed processing message:" + message);
        }
        logger.info("Consumed message -> {}", message);
    }

    @KafkaListener(id = "dlt-messages-container", groupId = "messages-dlt-group", topics = Config.TOPIC_DLT)
    public void dltConsume(String message) {
        logger.info("Received from {} : {}", Config.TOPIC_DLT, message);
    }


}

