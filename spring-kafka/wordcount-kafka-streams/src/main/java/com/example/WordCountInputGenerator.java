package com.example;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WordCountInputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(WordCountInputGenerator.class);

    private Faker faker = new Faker();
    private KafkaTemplate<String, String> kafkaTemplate;

    private WordCountConfig wordCountConfig;

    public WordCountInputGenerator(KafkaTemplate<String, String> kafkaTemplate, WordCountConfig wordCountConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.wordCountConfig = wordCountConfig;
    }

    @Scheduled(fixedRate = 3000)
    public void generatePayload() {
        String payload = faker.chuckNorris().fact();
        logger.info("sending to topic '{}' the payload '{}'", wordCountConfig.getInput().getName(), payload);
        kafkaTemplate.send(wordCountConfig.getInput().getName(), payload);
    }

}
