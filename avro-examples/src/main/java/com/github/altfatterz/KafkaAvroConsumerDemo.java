package com.github.altfatterz;

import com.github.altfatterz.avro.Customer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

public class KafkaAvroConsumerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaAvroConsumerDemo.class);

    static final String BOOTSTRAP_SERVERS = "localhost:9092";
    static final String GROUP_ID = "customer-consumer-app";
    static final String TOPIC = "customer-topic";

    public static void main(String[] args) {

        // common consumer configuration
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // avro specific configuration
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        properties.setProperty("specific.avro.reader", "true");
        properties.setProperty("schema.registry.url", "http://localhost:8081");

        // create the consumer
        KafkaConsumer<String, Customer> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(TOPIC));

        logger.info("waiting for data ...");

        while (true){
            ConsumerRecords<String, Customer> records = consumer.poll(1000);

            for (ConsumerRecord<String, Customer> record : records){
                Customer customer = record.value();
                logger.info("Customer: {}", customer);
            }
        }

    }
}
