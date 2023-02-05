package com.example;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(InputGenerator.class);

    private Faker faker = new Faker();
    private KafkaTemplate<String, String> kafkaTemplate;

    public InputGenerator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generatePayload() {
        String payload = faker.chuckNorris().fact();
        logger.info("sending to topic '{}' the payload '{}'", Config.INPUT_TOPIC, payload);
        kafkaTemplate.send(Config.INPUT_TOPIC, payload);
    }

}
