package com.example;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableScheduling
public class AmqStreamsProducer {

    public static void main(String[] args) {
        SpringApplication.run(AmqStreamsProducer.class, args);
    }

}

@Component
class Producer {

    private Faker faker = new Faker();
    private KafkaTemplate kafkaTemplate;

    public Producer(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 500)
    public void produce() {
        kafkaTemplate.send(Config.TOPIC, faker.chuckNorris().fact());
    }


}
