package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringBootApplication
public class SensorAverageApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorAverageApplication.class, args);
    }

    @Bean
    public Function<Flux<Sensor>, Flux<Average>> calculateAverage() {
        return data -> data.window(Duration.ofSeconds(5)).flatMap(
                window -> window.groupBy(Sensor::getId).flatMap(this::calculateAverage));
    }

    private Mono<Average> calculateAverage(GroupedFlux<Integer, Sensor> group) {
        return group
                .reduce(new Accumulator(0, 0),
                        (accumulator, sensor) -> new Accumulator(accumulator.getCount() + 1, accumulator.getTotalValue() + sensor.getTemperature()))
                .map(accumulator -> new Average(group.key(), accumulator.getTotalValue() / accumulator.getCount()));
    }


    //Following source and sinks are used for testing only.
    //Test source will send data to the same destination where the processor receives data
    //Test sink will consume data from the same destination where the processor produces data
    static class TestSource {

        private Random random = new Random();
        private int[] ids = new int[]{1001, 1002, 1003};

        @Bean
        public Supplier<Sensor> sendTestData() {

            return () -> {
                int id = ids[random.nextInt(3)];
                int temperature = random.nextInt(60) - 20;
                Sensor sensor = new Sensor();
                sensor.setId(id);
                sensor.setTemperature(temperature);
                return sensor;
            };
        }
    }

    static class TestSink {

        private final Logger logger = LoggerFactory.getLogger(TestSink.class);

        @Bean
        public Consumer<String> receive() {
            return payload -> logger.info("average:" + payload);
        }
    }

    static class Sensor {

        private int id;
        private int temperature;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTemperature() {
            return temperature;
        }

        public void setTemperature(int temperature) {
            this.temperature = temperature;
        }
    }

    static class Average {

        private int id;
        private double average;

        public Average(int id, double average) {
            this.id = id;
            this.average = average;
        }

        public int getId() {
            return id;
        }

        public double getAverage() {
            return average;
        }
    }

    static class Accumulator {

        private int count;
        private int totalValue;

        public Accumulator(int count, int totalValue) {
            this.count = count;
            this.totalValue = totalValue;
        }

        public int getCount() {
            return count;
        }


        public int getTotalValue() {
            return totalValue;
        }
    }


}
