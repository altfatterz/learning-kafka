package com.example;

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

	@KafkaListener(id = "messages-container", topics = Config.TOPIC)
	public void consume(String message) {
		logger.info("Consumed message -> {}", message);
	}

}