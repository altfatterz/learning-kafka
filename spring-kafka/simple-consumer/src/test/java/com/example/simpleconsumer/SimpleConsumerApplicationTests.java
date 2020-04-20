package com.example.simpleconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

// @EmbeddedKafka to register the EmbeddedKafkaBroker bean
// Note that here: a stand-alone (not Spring test context) broker will be created
@EmbeddedKafka
class SimpleConsumerApplicationTests {

	@Autowired
	private Consumer consumer;

	@Test
	void test(EmbeddedKafkaBroker embeddedKafka) {
		System.out.println("Embedded broker: " + embeddedKafka.getBrokersAsString());
		System.out.println("Zookeeper connection: "+ embeddedKafka.getZookeeperConnectionString());
	}

}
