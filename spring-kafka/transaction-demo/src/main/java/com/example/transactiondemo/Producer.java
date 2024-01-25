package com.example.transactiondemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private KafkaTemplate<String, String> kafkaTemplate;

    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void process1(String input) {
        kafkaTemplate.executeInTransaction(kafkaTemplate -> {
            String[] words = input.trim().split(" ");
            for (String word : words) {
                if (word.equals("fail")) throw new RuntimeException();
                kafkaTemplate.send(Config.WORDS, word);

                // Simulate slow processing
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
    }


    @Transactional
    public void process2(String input) throws InterruptedException {
        logger.info("process2 KafkaTemplate is transactional: " + kafkaTemplate.isTransactional());
        String[] words = input.trim().split(" ");
        for (String word : words) {
            if (word.equals("fail")) throw new RuntimeException();
            kafkaTemplate.send(Config.WORDS, word);
            // Simulate slow processing
            Thread.sleep(200);
        }
    }


}
