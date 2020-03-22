package com.example.simpleconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SimpleConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleConsumerApplication.class, args);
    }

}

@Component
class Consumer {

    private final Logger logger = LoggerFactory.getLogger(Consumer.class);

    @KafkaListener(groupId = "messages-group", topics = Config.TOPIC)
    public void consume(String message) {
        if (message.startsWith("fail")) {
            throw new RuntimeException("failed processing message:" + message);
        }
        logger.info("Consumed message -> {}", message);
    }

    @KafkaListener(groupId = "messages-dlt-group", topics = Config.TOPIC_DLT)
    public void dltConsume(String message) {
        // not sure yet why this is not called
        logger.info("Received from {} : {}", Config.TOPIC_DLT, message);
    }


}