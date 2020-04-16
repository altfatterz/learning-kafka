package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.Arrays;

@SpringBootApplication
@EnableKafkaStreams
public class WordCountApp {

    public static void main(String[] args) {
        SpringApplication.run(WordCountApp.class, args);
    }

    @Bean
    public KTable<String, Long> kStream(StreamsBuilder streamsBuilder) {

        KStream<String, String> sentences = streamsBuilder.stream(Config.INPUT_TOPIC);

        KTable<String, Long> wordCounts = sentences
                .mapValues(sentence -> sentence.toLowerCase())
                .flatMapValues(sentence -> Arrays.asList(sentence.split("\\W+")))
                .selectKey((key, word) -> word)
                .groupByKey()
                .count();

        sentences.to("word-count-output");

        return wordCounts;
    }

}
