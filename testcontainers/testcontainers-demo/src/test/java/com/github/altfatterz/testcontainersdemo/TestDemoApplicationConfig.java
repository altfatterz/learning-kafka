package com.github.altfatterz.testcontainersdemo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestDemoApplicationConfig {

	@Bean
	@ServiceConnection
	// https://java.testcontainers.org/modules/databases/postgres/
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
				.withDatabaseName("test").withUsername("postgres").withPassword("secret");
	}

	@Bean
//	@ServiceConnection
	// ServiceConnection is needed to wire up the connection with the application
	// https://java.testcontainers.org/modules/databases/mongodb/
	MongoDBContainer mongoDBContainer() {
		return new MongoDBContainer("mongo:7");
	}

	@Bean
	// https://java.testcontainers.org/modules/kafka/
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).withKraft();
	}


}
