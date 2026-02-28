package com.github.altfatterz;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
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

        final Thread mainThread = Thread.currentThread();

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Detected shutdown, signaling consumer to wakeup...");
            consumer.wakeup();
            try {
                mainThread.join(); // Wait for main thread to finish its finally block
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try {
            final long startTimestamp = Long.parseLong(props.getProperty("read-from-timestamp"));
            logger.info("consumer subscribe with ConsumerRebalanceListener");
            consumer.subscribe(Collections.singletonList(topic), new CustomConsumerRebalanceListener(consumer, startTimestamp));
        } catch (NumberFormatException e) {
            logger.info("Could not parse `read-from-timestamp` property using instead `auto.offset.reset` property");
            // subscribe consumer to topic
            consumer.subscribe(Collections.singletonList(topic));
        }

        // Get metadata about the partitions for a given topic
        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
        for (PartitionInfo partitionInfo : partitionInfos) {
            logger.info("PartitionInfo: {}", partitionInfo);
        }

        // poll for new data
        try {
            while (true) {
                // max.poll.records (default 500)
                // poll() will throw WakeupException if wakeup() was called
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

                logger.info("poll() returned nr of records:{}", records.count());

                for (ConsumerRecord<String, String> record : records) {
                    logger.info("Key: {},  Partition: {}, Offset: {}, Value:{},", record.key(), record.partition(),
                            record.offset(), record.value());
                }
            }
        } catch (WakeupException e) {
            // This is expected on shutdown, ignore it
            logger.info("Consumer received wakeup signal.");
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        } finally {
            // This is now safe because it's running in the MAIN thread
            logger.info("Closing consumer safely...");
            consumer.close();
            logger.info("Consumer closed.");
        }
    }
}
