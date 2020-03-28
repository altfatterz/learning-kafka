package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TransactionalProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionalProducerApplication.class, args);
    }

}

@RestController
class TransactionalProducer {

    private final Logger logger = LoggerFactory.getLogger(TransactionalProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionalProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/messages")
    public void send(@RequestBody String message) {

        // Why is not working? it gets timeout: Timeout expired after 60000milliseconds while awaiting InitProducerId
        kafkaTemplate.executeInTransaction(t -> {
            String[] words = message.trim().split(" ");
            for (int i = 0; i < words.length; i++) {
                kafkaTemplate.send(Config.TOPIC, words[i]);
            }
            return true;
        });
    }

    @PostMapping("/messages2")
    @Transactional
    public void send2(@RequestBody String message) {

        // Why is not working? it gets timeout: Timeout expired after 60000milliseconds while awaiting InitProducerId
        //kafkaTemplate.executeInTransaction(t -> {
            String[] words = message.trim().split(" ");
            for (int i = 0; i < words.length; i++) {
                kafkaTemplate.send(Config.TOPIC, words[i]);
            }
        //    return true;
        //});
    }
}