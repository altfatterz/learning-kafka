package com.example;

import com.example.avro.StockTrade;
import com.github.javafaker.Faker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableScheduling
public class AmqStreamsProducer {

    private Faker faker = new Faker();

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
        StockTrade stockTrade = new StockTrade(
              buyOrSell(faker),
                faker.number().numberBetween(1, 5000),
                faker.stock().nsdqSymbol(),
                faker.number().randomDouble(2, 5, 1000),
                "User_" + faker.number().digits(1)
        );
        kafkaTemplate.send(Config.TOPIC, stockTrade);
    }

    private String buyOrSell(Faker faker) {
        if (faker.bool().bool()) return "BUY"; else return "SELL";
    }


}
