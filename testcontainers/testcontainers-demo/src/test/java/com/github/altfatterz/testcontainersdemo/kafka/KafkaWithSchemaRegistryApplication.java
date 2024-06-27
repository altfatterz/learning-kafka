package com.github.altfatterz.testcontainersdemo.kafka;

import com.github.altfatterz.testcontainersdemo.TestcontainersDemoApplication;
import org.springframework.boot.SpringApplication;

public class KafkaWithSchemaRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestcontainersDemoApplication::main)
                .with(KafkaWithSchemaRegistryConfiguration.class).run(args);
    }
}
