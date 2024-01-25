package com.example.transactiondemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class Consumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public Consumer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(groupId = "words", topics = Config.WORDS)
    public void processWords(List<String> words) {
        logger.info("Received: " + words);
        logger.info("processWords KafkaTemplate is transactional: " + kafkaTemplate.isTransactional());
        words.forEach(word -> kafkaTemplate.send(Config.UPPERCASE_WORDS, word.toUpperCase()));
    }

    @KafkaListener(groupId = "uppercase_words", topics = Config.UPPERCASE_WORDS)
    public void processUppercaseWords(List<String> uppercaseWords) {
        logger.info("Received: " + uppercaseWords);
    }

}
