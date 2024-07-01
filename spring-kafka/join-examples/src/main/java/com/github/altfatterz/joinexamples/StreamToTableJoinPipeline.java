package com.github.altfatterz.joinexamples;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamToTableJoinPipeline {

    private final StreamToTableJoinConfig config;

    private static final Logger logger = LoggerFactory.getLogger(StreamToTableJoinPipeline.class);

    public StreamToTableJoinPipeline(StreamToTableJoinConfig config) {
        this.config = config;
    }

    @Bean
    public KTable<String, Long> buildPipeline(StreamsBuilder streamsBuilder) {

        KStream<String, Long> userClicksStream = streamsBuilder.stream(config.getInput1().getName(),
                Consumed.with(Serdes.String(), Serdes.Long()));

        KTable<String, String> userRegionsTable = streamsBuilder.table(config.getInput2().getName(),
                Consumed.with(Serdes.String(), Serdes.String()));

        KTable<String, Long> clicksPerRegion = userClicksStream.leftJoin(
                        userRegionsTable, (readOnlyKey, clicks, region) -> {
                            logger.info("inside value joiner with key: " + readOnlyKey);
                            return new RegionWithClicks(region == null ? "UNKNOWN" : region, clicks);
                        }
                        // Change the stream from <user> -> <region, clicks> to <region> -> <clicks>
                ).map((user, regionWithClicks) ->
                                new KeyValue<>(regionWithClicks.getRegion(), regionWithClicks.getClicks()),
                        Named.as("region-to-clicks-mapper")
                        // Compute the total per region by summing the individual click counts per region.
                ).groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .reduce(Long::sum, Materialized.as("region-to-clicks-reducer"));

        // Write the (continuously updating) results to the output topic.
        clicksPerRegion.toStream().to(config.getOutput().getName(), Produced.with(Serdes.String(), Serdes.Long()));

        return clicksPerRegion;
    }
}

