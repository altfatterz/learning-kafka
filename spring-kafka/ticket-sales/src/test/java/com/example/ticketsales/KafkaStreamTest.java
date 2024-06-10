package com.example.ticketsales;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.Test;
//import org.springframework.kafka.streams.messaging.MessagingTransformer;
import org.springframework.kafka.support.SimpleKafkaHeaderMapper;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaStreamTest {

    private static final String INPUT = "input";

    private static final String OUTPUT = "output";

//    @Test
//    void testWithDriver() {
//        MessagingMessageConverter converter = new MessagingMessageConverter();
//        converter.setHeaderMapper(new SimpleKafkaHeaderMapper("*"));
//        MessagingTransformer<String, String, String> messagingTransformer = new MessagingTransformer<>(message ->
//                MessageBuilder.withPayload("bar".getBytes())
//                        .copyHeaders(message.getHeaders())
//                        .setHeader("baz", "qux".getBytes())
//                        .build(),
//                converter);
//        StreamsBuilder builder = new StreamsBuilder();
//        KStream<String, String> stream = builder.stream(INPUT);
//        stream
//                .transform(() -> messagingTransformer)
//                .to(OUTPUT);
//
//        Properties config = new Properties();
//        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
//        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9999");
//        TopologyTestDriver driver = new TopologyTestDriver(builder.build(), config);
//
//        TestInputTopic<String, String> inputTopic = driver.createInputTopic(INPUT, new StringSerializer(),
//                new StringSerializer());
//        Headers headers = new RecordHeaders(Collections.singletonList(new RecordHeader("fiz", "buz".getBytes())));
//        inputTopic.pipeInput(new TestRecord<>("key", "value", headers));
//        TestOutputTopic<byte[], byte[]> outputTopic = driver.createOutputTopic(OUTPUT, new ByteArrayDeserializer(),
//                new ByteArrayDeserializer());
//        TestRecord<byte[], byte[]> result = outputTopic.readRecord();
//        assertThat(result.value()).isEqualTo("bar".getBytes());
//        assertThat(result.headers().lastHeader("fiz").value()).isEqualTo("buz".getBytes());
//        assertThat(result.headers().lastHeader("baz").value()).isEqualTo("qux".getBytes());
//        driver.close();
//    }
}
