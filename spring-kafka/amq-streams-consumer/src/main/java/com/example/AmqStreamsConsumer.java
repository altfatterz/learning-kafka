package com.example;

import com.example.avro.StockTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AmqStreamsConsumer {

	public static void main(String[] args) {
		SpringApplication.run(AmqStreamsConsumer.class, args);
	}

}

@Component
class Consumer {

	private final Logger logger = LoggerFactory.getLogger(Consumer.class);

	@KafkaListener(topics = Config.TOPIC)
	public void consume(StockTrade stockTrade) {
		logger.info("StockTrade: side={}, symbol={}, quantity= {}, price={}",
				stockTrade.getSide(), stockTrade.getSymbol(), stockTrade.getQuantity(), stockTrade.getPrice());
	}

}