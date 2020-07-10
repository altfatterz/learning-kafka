package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SecureKafkaProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureKafkaProducerApplication.class, args);
    }

}

@RestController
class Producer {

    private static final String TOPIC = "secure-topic";
    private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        String payload = message.trim();
        LOGGER.info("Sending payload {}", payload);
        kafkaTemplate.send(TOPIC, payload);
    }
}
