package com.example.springkafkatesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendFact(String topic, String fact) {
        logger.info("sending to topic '{}' the payload '{}'", topic, fact);
        kafkaTemplate.send(topic, fact);
        kafkaTemplate.flush();
    }


}
