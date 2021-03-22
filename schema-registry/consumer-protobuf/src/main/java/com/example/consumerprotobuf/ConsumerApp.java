package com.example.consumerprotobuf;

import com.example.model.Customer.CustomerOuterClass.Customer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConsumerApp {

    private static final String KAFKA_TOPIC = "demo-topic-protobuf";
    private static final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);

    public static void main(String[] args) throws InterruptedException {

        logger.info("Starting Java Protobuf consumer.");

        final Properties settings = new Properties();

        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-app-protobuf10");
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);

        settings.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, Customer.class.getName());
        settings.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        settings.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, List.of(MonitoringConsumerInterceptor.class));

        final KafkaConsumer<String, Customer> consumer = new KafkaConsumer<>(settings);

        try {
            // Subscribe to our topic
            consumer.subscribe(Arrays.asList(KAFKA_TOPIC));
            while (true) {
                final ConsumerRecords<String, Customer> records =
                        consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Customer> record : records) {
                    logger.info("Key:{} first-name:{} last-name: {}, [partition {}]",
                            record.key(),
                            record.value().getFirstName(),
                            record.value().getLastName(),
                            record.partition());
                }
            }
        } finally {
            // Clean up when the application exits or errors
            logger.info("Closing consumer.");
            consumer.close();
        }


    }

}
