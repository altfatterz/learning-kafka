package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class WordCountPipelineTest {

    private TopologyTestDriver topologyTestDriver;

    private WordCountConfig wordCountConfig = new WordCountConfig();

    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, Long> outputTopic;

    // under test
    private WordCountPipeline wordCountPipeline;

    @BeforeEach
    void beforeEach() {
        wordCountConfig.setInput(new WordCountConfig.Topic("input"));
        wordCountConfig.setOutput(new WordCountConfig.Topic("output"));

        wordCountPipeline = new WordCountPipeline(wordCountConfig);

        // Create topology to handle stream of users
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        wordCountPipeline.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        // Create test driver
        topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic(wordCountConfig.getInput().getName(),
                Serdes.String().serializer(), Serdes.String().serializer());

        outputTopic = topologyTestDriver.createOutputTopic(wordCountConfig.getOutput().getName(),
                Serdes.String().deserializer(), Serdes.Long().deserializer());

    }

    @AfterEach
    void afterEach() {
        topologyTestDriver.close();
    }

    @Test
    public void wordCount() {
        // arrange
        List<String> input = asList(
                "Chuck Norris doesn't need an OS.",
                "The Swiss Army uses Chuck Norris Knives",
                "Chuck Norris has a diary, it is called the Guinness Book Of World Records"
        );

        // act
        inputTopic.pipeValueList(input);

        // assert
        List<KeyValue<String, Long>> output = outputTopic.readKeyValuesToList();

        List<KeyValue<String, Long>> expectedOutput = List.of(
                KeyValue.pair("norris", 1L),
                KeyValue.pair("norris", 2L),
                KeyValue.pair("knives", 1L),
                KeyValue.pair("norris", 3L),
                KeyValue.pair("called", 1L),
                KeyValue.pair("guinness", 1L),
                KeyValue.pair("records", 1L)
        );

        assertThat(output).isEqualTo(expectedOutput);

    }

}
