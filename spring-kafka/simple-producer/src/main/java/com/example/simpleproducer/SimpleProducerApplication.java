package com.example.simpleproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SimpleProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleProducerApplication.class, args);
    }

}

@RestController
class Producer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        kafkaTemplate.send(Config.TOPIC, message);
    }
}