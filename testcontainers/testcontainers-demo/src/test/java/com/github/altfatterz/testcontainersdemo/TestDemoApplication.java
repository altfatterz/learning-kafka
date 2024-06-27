package com.github.altfatterz.testcontainersdemo;

import org.springframework.boot.SpringApplication;

public class TestDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(TestcontainersDemoApplication::main)
                .with(TestDemoApplicationConfig.class).run(args);
    }

}
