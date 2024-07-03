package com.github.altfatterz.joinexamples;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class StreamToStreamJoinPipeline {

    private StreamToStreamJoinConfig config;
    private StreamToStreamValueJoiner joiner;

    private static final Logger logger = LoggerFactory.getLogger(StreamToStreamJoinPipeline.class);

    public StreamToStreamJoinPipeline(StreamToStreamJoinConfig config, StreamToStreamValueJoiner joiner) {
        this.config = config;
        this.joiner = joiner;
    }

    @Bean(name = "stream-to-stream")
    public KStream<String, String> buildPipeline(StreamsBuilder streamsBuilder) {

        KStream<String, String> adImpressions = streamsBuilder.stream(config.getInput1(),
                Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> adClicks = streamsBuilder.stream(config.getInput2(),
                Consumed.with(Serdes.String(), Serdes.String()));

        // Inner Join - only if both sides are available within the defined time window a joined result emitted
        KStream<String, String> adImpressionsAndClicks = adImpressions
                .peek((key, value) -> logger.info("key: {}, value: {}", key, value))
                .outerJoin(adClicks, joiner,
                        // KStream-KStream joins are always windowed joins, hence we must provide a join window.
                        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(5)),
                        StreamJoined.with(
                                Serdes.String(), /* key */
                                Serdes.String(), /* left value */
                                Serdes.String()  /* right value */
                        )
                ).peek((key, value) -> logger.info("key: {}, value: {}", key, value));

        // Write the results to the output topic.
        adImpressionsAndClicks.to(config.getOutput(), Produced.with(Serdes.String(), Serdes.String()));

        return adImpressionsAndClicks;
    }
}

