package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.Arrays;

@SpringBootApplication
@EnableKafkaStreams
public class WordCountApp {

    public static void main(String[] args) {
        SpringApplication.run(WordCountApp.class, args);
    }

    @Bean
    public KStream<String, Long> kStream(StreamsBuilder streamsBuilder) {

        KStream<String, String> sentences = streamsBuilder.stream(Config.INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, Long> wordCount = sentences.mapValues(sentence -> sentence.toLowerCase())
                .flatMapValues(sentence -> Arrays.asList(sentence.split("\\W+")))
                .selectKey((key, word) -> word)
                .groupByKey()  // KGroupedStream<String, String>
                .count()
                .toStream();

        //wordCount.to(Config.OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        return wordCount;
    }

}
