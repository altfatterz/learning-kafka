package com.github.altfatterz;

import com.github.altfatterz.types.Product;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaJsonSchemaConsumerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaJsonSchemaConsumerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        final KafkaConsumer<String, Product> consumer = new KafkaConsumer<>(props);

        try {
            // Subscribe to our topic
            consumer.subscribe(Arrays.asList(topic));
            while (true) {
                final ConsumerRecords<String, Product> records =
                        consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Product> record : records) {
                    logger.info("Key:{} product: {}, [partition {}]",
                            record.key(),
                            record.value(),
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
