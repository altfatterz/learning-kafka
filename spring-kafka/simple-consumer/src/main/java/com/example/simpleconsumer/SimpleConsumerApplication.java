package com.example.simpleconsumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class SimpleConsumerApplication {

    private static final String TOPIC = "messages";

    private static final String TOPIC_DLT = "messages.DLT";

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicDLT() {
        return TopicBuilder.name(TOPIC_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

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
        logger.info("Review Listener Containers:");
        return args -> registry.getAllListenerContainers().forEach(messageListenerContainer -> {
            logger.info("listenerId={},groupId={}",
                    messageListenerContainer.getListenerId(),
                    messageListenerContainer.getGroupId());
        });
    }

    @GetMapping("/containers")
    public Set<String> containers() {
        return registry.getListenerContainers().stream()
                .map(c -> c.getListenerId() + ":" + c.isRunning())
                .collect(Collectors.toSet());
    }

    @KafkaListener(groupId = "messages-group", topics = TOPIC,
            clientIdPrefix="${spring.application.name}", concurrency = "3")
    public void consume(String message) {
        if (message.startsWith("poison-pill")) {
            throw new RuntimeException("failed processing message:" + message);
        }
        logger.info("Consumed message -> {}", message);
    }

    @KafkaListener(groupId = "messages-dlt-group", topics = TOPIC_DLT,
            clientIdPrefix = "${spring.application.name}")
    public void dltConsume(String message) {
        logger.info("Received from {} : {}", TOPIC_DLT, message);
    }

}


