package com.github.altfatterz.joinexamples;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class StreamToStreamJoinPipelineTest {

    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, String> inputTopic1;
    private TestInputTopic<String, String> inputTopic2;
    private TestOutputTopic<String, String> outputTopic;

    // dependencies
    private StreamToStreamValueJoiner joiner = new StreamToStreamValueJoiner();
    private StreamToStreamJoinConfig config = new StreamToStreamJoinConfig();

    // under test
    private StreamToStreamJoinPipeline streamToStreamJoinPipeline;

    @BeforeEach
    void beforeEach() {
        config.setInput1("input1");
        config.setInput2("input2");
        config.setOutput("output");
        config.setWindowSizeInSeconds(5);

        streamToStreamJoinPipeline = new StreamToStreamJoinPipeline(config, joiner);

        // Create topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamToStreamJoinPipeline.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        // Create test driver
        topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic1 = topologyTestDriver.createInputTopic(config.getInput1(),
                Serdes.String().serializer(), Serdes.String().serializer());

        inputTopic2 = topologyTestDriver.createInputTopic(config.getInput2(),
                Serdes.String().serializer(), Serdes.String().serializer());

        outputTopic = topologyTestDriver.createOutputTopic(config.getOutput(),
                Serdes.String().deserializer(), Serdes.String().deserializer());
    }

    @AfterEach
    void afterEach() {
        topologyTestDriver.close();
    }

    @Test
    public void shouldCountClicksPerRegion() {
        Instant now = Instant.now();

        inputTopic1.pipeInput("car-advertisement", "shown", now);
        inputTopic1.pipeInput("newspaper-advertisement", "shown",
                now.plusSeconds(config.getWindowSizeInSeconds() + 2));

        inputTopic2.pipeInput("newspaper-advertisement", "clicked",
                now.plusSeconds(config.getWindowSizeInSeconds() + 1));

        final List<KeyValue<String, String>> expectedResults = Arrays.asList(
                new KeyValue<>("car-advertisement", "shown/not-clicked-yet"),
                new KeyValue<>("newspaper-advertisement", "shown/clicked")
        );

        assertThat(outputTopic.readKeyValuesToList()).isEqualTo(expectedResults);

    }

}
