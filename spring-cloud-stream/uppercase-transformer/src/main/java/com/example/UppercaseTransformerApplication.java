package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringBootApplication
public class UppercaseTransformerApplication {

    private static Logger logger = LoggerFactory.getLogger(UppercaseTransformerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(UppercaseTransformerApplication.class, args);
    }

    // by default the `input` and `output` binding names will be `transform-in-0` and `transform-out-0`
    @Bean
    public Function<String, String> transform() {
        return String::toUpperCase;
    }

    // source semantics exposed as java.util.function.Supplier
    static class TestSource {

        private AtomicBoolean semaphore = new AtomicBoolean(true);

        // will send: foo,bar,foo,bar,..
        // the source (the origin) of the data, it does not subscribe to any in-bound destination and,
        // therefore, has to be triggered by some other mechanism
        // can be imperative or reactive - which relates how are they triggered
        @Bean
        public Supplier<String> sendTestData() {
            // produces a string whenever its get() method is called
            // the framework will call this supplier by default every 1 second
            return () -> this.semaphore.getAndSet(!this.semaphore.get()) ? "foo" : "bar";

        }
    }

    // sink semantics exposed as java.util.function.Consumer
    static class TestSink {

        @Bean
        public Consumer<String> receive() {
            return payload -> logger.info("Data received: " + payload);
        }
    }

}
