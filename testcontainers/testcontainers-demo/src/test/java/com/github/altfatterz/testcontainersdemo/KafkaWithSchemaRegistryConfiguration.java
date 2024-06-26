package com.github.altfatterz.testcontainersdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
// to avoid getting error with providing the url to the database
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class KafkaWithSchemaRegistryConfiguration {


    //  Starting a Kafka container with a schema registry requires some more work,
    //  as there is still no official support for a schema registry test container.

    // To get both containers communicating, we need to create a docker network and connect both containers to it
    private static final Network NETWORK = Network.newNetwork();

    // https://java.testcontainers.org/modules/kafka/
    @Bean
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                .withKraft()
                .withNetwork(NETWORK);
    }

    @Bean
    GenericContainer<?> schemaRegistry(KafkaContainer kafkaContainer) {
        return new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.6.1"))
                .withNetwork(NETWORK)
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                        "PLAINTEXT://" + kafkaContainer.getNetworkAliases().get(0) + ":9092")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    }

}