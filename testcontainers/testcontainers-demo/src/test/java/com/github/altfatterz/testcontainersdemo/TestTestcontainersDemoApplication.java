package com.github.altfatterz.testcontainersdemo;

import org.springframework.boot.SpringApplication;

public class TestTestcontainersDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestcontainersDemoApplication::main)
                .with(TestcontainersConfiguration.class).run(args);
    }

}
