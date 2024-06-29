package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.regex.Pattern;

@Configuration
public class WordCountPipeline {

    private final WordCountConfig wordCountConfig;

    private static final Logger logger = LoggerFactory.getLogger(WordCountPipeline.class);

    public WordCountPipeline(WordCountConfig wordCountConfig) {
        this.wordCountConfig = wordCountConfig;
    }

    @Bean
    public KStream<String, Long> buildPipeline(StreamsBuilder streamsBuilder) {
        // Construct a `KStream` from the input topic Config.INPUT_TOPIC, where message values
        // represent lines of text (for the sake of this example, we ignore whatever may be stored
        // in the message keys)
        final KStream<byte[], String> textLines = streamsBuilder.stream(wordCountConfig.getInput().getName(),
                Consumed.with(Serdes.ByteArray(), Serdes.String()));

        final Pattern pattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS);

        KStream<String, Long> result = textLines
                .peek((key, value) -> logger.info("input record - key: {} value: {}", key, value))
                // Split each text line, by whitespace, into words.  The text lines are the record
                // values, i.e. we can ignore whatever data is in the record keys and thus invoke
                // `flatMapValues()` instead of the more generic `flatMap()`.
                .flatMapValues(value -> Arrays.asList(pattern.split(value.toLowerCase())))
                .peek((key, value) -> logger.info("after flatMapValues - key: {} value: {}", key, value))
                // we consider only words longer than 5 characters
                .filter((bytes, value) -> value.length() > 5)
                .peek((key, value) -> logger.info("after filter - key: {} value: {}", key, value))
                // Group the split data by word so that we can subsequently count the occurrences per word.
                // This step re-keys (re-partitions) the input data, with the new record key being the words.
                // Note: No need to specify explicit serdes because the resulting key and value types
                // (String and String) match the application's default serdes.
                .groupBy((keyIgnored, word) -> word, Grouped.with(Serdes.String(), Serdes.String()))
                // Count the occurrences of each word (record key).
                .count(Materialized.with(Serdes.String(), Serdes.Long()))
                // change the `KTable<String, Long>` to a KStream<String, Long> to write to the output topic.
                .toStream()
                .peek((key, value) -> logger.info("output record - key: {} value: {}", key, value));

        result.to(wordCountConfig.getOutput().getName(), Produced.with(Serdes.String(), Serdes.Long()));

        return result;
    }
}
