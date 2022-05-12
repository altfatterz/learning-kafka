package com.github.altfatterz;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaConsumerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaConsumerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // subscribe consumer to topic
        consumer.subscribe(Arrays.asList(topic));

        // poll for new data
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

            for (ConsumerRecord<String, String> record : records) {
                logger.info("Key: {}, Value:{}, Partition: {}, Offset: {}", record.key(),
                        record.value(), record.partition(), record.offset());
            }
        }
    }
}
