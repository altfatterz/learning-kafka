package com.example.springkafkatesting;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    @Value("${topic}")
    private String topic;

    private Faker faker = new Faker();
    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generatePayload() {
        String payload = faker.chuckNorris().fact();
        logger.info("sending to topic '{}' the payload '{}'", topic, payload);
        kafkaTemplate.send(topic, payload);
    }

    public void sendFact(String topic, String fact) {
        logger.info("sending to topic '{}' the payload '{}'", topic, fact);
        kafkaTemplate.send(topic, fact);
    }


}
