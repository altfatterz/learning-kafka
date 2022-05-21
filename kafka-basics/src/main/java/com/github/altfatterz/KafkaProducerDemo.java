package com.github.altfatterz;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class KafkaProducerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaProducerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing producer.");
            producer.close();
        }));

        // create a producer record
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, new Date().toString());

        logger.info("send message asynchronously....");
        producer.send(record);

    }

}
