package com.github.altfatterz.joinexamples;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreamToTableJoinPipelineTest {

    private TopologyTestDriver topologyTestDriver;

    private StreamToTableJoinConfig config = new StreamToTableJoinConfig();

    private TestInputTopic<String, Long> inputTopic1;
    private TestInputTopic<String, String> inputTopic2;

    private TestOutputTopic<String, Long> outputTopic;

    // under test
    private StreamToTableJoinPipeline streamToTableJoinPipeline;

    @BeforeEach
    void beforeEach() {
        config.setInput1("input1");
        config.setInput2("input2");
        config.setOutput("output");

        streamToTableJoinPipeline = new StreamToTableJoinPipeline(config);

        // Create topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamToTableJoinPipeline.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        // Create test driver
        topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic1 = topologyTestDriver.createInputTopic(config.getInput1(),
                Serdes.String().serializer(), Serdes.Long().serializer());

        inputTopic2 = topologyTestDriver.createInputTopic(config.getInput2(),
                Serdes.String().serializer(), Serdes.String().serializer());

        outputTopic = topologyTestDriver.createOutputTopic(config.getOutput(),
                Serdes.String().deserializer(), Serdes.Long().deserializer());
    }

    @AfterEach
    void afterEach() {
        topologyTestDriver.close();
    }

    @Test
    public void shouldCountClicksPerRegion() {
        // Input 1: Clicks per user (multiple records allowed per user).
        List<KeyValue<String, Long>> userClicks = Arrays.asList(
                new KeyValue<>("alice", 13L),
                new KeyValue<>("bob", 4L),
                new KeyValue<>("chao", 25L),
                new KeyValue<>("bob", 19L),
                new KeyValue<>("dave", 56L),
                new KeyValue<>("eve", 78L),
                new KeyValue<>("alice", 40L),
                new KeyValue<>("fang", 99L)
        );

        // Input 2: Region per user (multiple records allowed per user).
        List<KeyValue<String, String>> userRegions = Arrays.asList(
                new KeyValue<>("alice", "asia"),   /* Alice lived in Asia originally... */
                new KeyValue<>("bob", "americas"),
                new KeyValue<>("chao", "asia"),
                new KeyValue<>("dave", "europe"),
                new KeyValue<>("alice", "europe"), /* ...but moved to Europe some time later. */
                new KeyValue<>("eve", "americas"),
                new KeyValue<>("fang", "asia")
        );

        Map<String, Long> expected = Map.of("asia", 124L, "europe", 109L, "americas", 101L);

        // need to populate first the table, otherwise region will be unknown
        inputTopic2.pipeKeyValueList(userRegions);

        inputTopic1.pipeKeyValueList(userClicks);

        assertThat(outputTopic.readKeyValuesToMap()).isEqualTo(expected);
    }

    @Test
    public void clicksWithoutRegion() {
        // Input 1: Clicks per user (multiple records allowed per user).
        List<KeyValue<String, Long>> userClicks = Arrays.asList(
                new KeyValue<>("alice", 13L),
                new KeyValue<>("bob", 4L),
                new KeyValue<>("chao", 25L),
                new KeyValue<>("bob", 19L),
                new KeyValue<>("dave", 56L),
                new KeyValue<>("eve", 78L),
                new KeyValue<>("alice", 40L),
                new KeyValue<>("fang", 99L)
        );

        Map<String, Long> expected = Map.of("UNKNOWN", 334L);

        inputTopic1.pipeKeyValueList(userClicks);

        assertThat(outputTopic.readKeyValuesToMap()).isEqualTo(expected);
    }

}
