package com.github.altfatterz.testcontainersdemo;

import org.springframework.boot.SpringApplication;

public class KafkaWithSchemaRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestcontainersDemoApplication::main)
                .with(KafkaWithSchemaRegistryConfiguration.class).run(args);
    }
}
