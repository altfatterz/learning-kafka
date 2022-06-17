package com.example.springkafkatesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringKafkaTestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaTestingApplication.class, args);
    }

}
