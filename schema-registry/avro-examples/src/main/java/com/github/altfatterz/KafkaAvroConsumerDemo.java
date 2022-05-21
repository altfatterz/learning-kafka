package com.github.altfatterz;

import com.github.altfatterz.avro.Customer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaAvroConsumerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaAvroConsumerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // create the consumer
        KafkaConsumer<String, Customer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));

        logger.info("waiting for data ...");

        while (true){
            ConsumerRecords<String, Customer> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, Customer> record : records){
                Customer customer = record.value();
                logger.info("Customer: {}", customer);
            }
        }

    }
}
