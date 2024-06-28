package com.example.ticketsales;

import io.confluent.developer.avro.TicketSale;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import java.util.List;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link TicketSalesPipeline}
 */
@ExtendWith(MockitoExtension.class)
class TicketSalesPipelineTest {

    private static final String SCHEMA_REGISTRY_SCOPE = TicketSalesPipelineTest.class.getName();
    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;
    private TopologyTestDriver topologyTestDriver;

    private TicketSalesConfig ticketSalesConfig = new TicketSalesConfig();

    private KafkaProperties kafkaProperties = Mockito.mock(KafkaProperties.class, Mockito.RETURNS_DEEP_STUBS);

    private TestInputTopic<String, TicketSale> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    // under test
    private TicketSalesPipeline ticketSalesPipeline;


    @BeforeEach
    void beforeEach() {
        ticketSalesConfig.setInput(new TicketSalesConfig.Topic("input"));
        ticketSalesConfig.setOutput(new TicketSalesConfig.Topic("output"));

        Mockito.when(kafkaProperties.getStreams().getProperties().
                get(SCHEMA_REGISTRY_URL_CONFIG)).thenReturn(MOCK_SCHEMA_REGISTRY_URL);

        ticketSalesPipeline = new TicketSalesPipeline(kafkaProperties, ticketSalesConfig);

        // Create topology to handle stream of users
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        ticketSalesPipeline.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        // Create test driver
        topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        // Create input and output topics:
        SpecificAvroSerde<TicketSale> ticketSaleSerde = TestUtils.getTicketSaleSerde(MOCK_SCHEMA_REGISTRY_URL);

        inputTopic = topologyTestDriver.createInputTopic(ticketSalesConfig.getInput().getName(),
                Serdes.String().serializer(), ticketSaleSerde.serializer());

        outputTopic = topologyTestDriver.createOutputTopic(ticketSalesConfig.getOutput().getName(),
                Serdes.String().deserializer(), Serdes.String().deserializer());

    }

    @AfterEach
    void afterEach() {
        topologyTestDriver.close();
        MockSchemaRegistry.dropScope(SCHEMA_REGISTRY_SCOPE);
    }

    @Test
    public void countSales() {
        // arrange
        List<TicketSale> input = asList(
                new TicketSale("Die Hard", "2019-07-18T10:00:00Z", 12),
                new TicketSale("Die Hard", "2019-07-18T10:01:00Z", 12),
                new TicketSale("The Godfather", "2019-07-18T10:01:31Z", 12),
                new TicketSale("Die Hard", "2019-07-18T10:01:36Z", 24),
                new TicketSale("The Godfather", "2019-07-18T10:02:00Z", 18),
                new TicketSale("The Big Lebowski", "2019-07-18T11:03:21Z", 12),
                new TicketSale("The Big Lebowski", "2019-07-18T11:03:50Z", 12),
                new TicketSale("The Godfather", "2019-07-18T11:40:00Z", 36),
                new TicketSale("The Godfather", "2019-07-18T11:40:09Z", 18)
        );

        // act
        inputTopic.pipeValueList(input);

        // assert
        List<KeyValue<String, String>> output = outputTopic.readKeyValuesToList();

        String suffix = " tickets sold";

        List<KeyValue<String, String>> expectedOutput = List.of(
                KeyValue.pair("Die Hard", "1" + suffix ),
                KeyValue.pair("Die Hard", "2" + suffix ),
                KeyValue.pair("The Godfather", "1" + suffix ),
                KeyValue.pair("Die Hard", "3" + suffix ),
                KeyValue.pair("The Godfather", "2" + suffix ),
                KeyValue.pair("The Big Lebowski", "1" + suffix ),
                KeyValue.pair("The Big Lebowski", "2" + suffix ),
                KeyValue.pair("The Godfather", "3" + suffix ),
                KeyValue.pair("The Godfather", "4" + suffix )
                );

        assertThat(output).isEqualTo(expectedOutput);
    }


}
