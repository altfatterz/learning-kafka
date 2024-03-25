package com.github.altfatterz;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class KafkaConsumerDemo {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing consumer.");
            consumer.close();
        }));

        try {
            final long startTimestamp = Long.parseLong(props.getProperty("read-from-timestamp"));
            // consumer.subscribe with ConsumerRebalanceListener
            consumer.subscribe(Arrays.asList(topic), new CustomCunsumerRebalanceListener(consumer, startTimestamp));
        } catch (NumberFormatException e) {
            logger.info("Could not parse `read-from-timestamp` property using instead `auto.offset.reset` property");
            // subscribe consumer to topic
            consumer.subscribe(Arrays.asList(topic));
        }

        // Get metadata about the partitions for a given topic
        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
        for (PartitionInfo partitionInfo : partitionInfos) {
            logger.info("PartitionInfo: {}", partitionInfo);
        }

        // poll for new data
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

            logger.info("Fetched nr of records:" + records.count());

            for (ConsumerRecord<String, String> record : records) {
                logger.info("Key: {},  Partition: {}, Offset: {}, Value:{},", record.key(), record.partition(),
                        record.offset(), record.value() );
            }
        }
    }
}
