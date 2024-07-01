package com.github.altfatterz.joinexamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafkaStreams
public class JoinExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoinExamplesApplication.class, args);
    }

}
