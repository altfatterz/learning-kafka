package com.example;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.time.Duration.ofSeconds;

public class TransactionalWordCount {

    static final Logger logger = LoggerFactory.getLogger(TransactionalWordCount.class);

    private static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static final String CONSUMER_GROUP_ID = "my-group-id";
    private static final String TRANSACTIONAL_ID = "transactional-wordcount-1";
    private static final String INPUT_TOPIC = "input-topic";
    private static final String OUTPUT_TOPIC = "output-topic";

    public static void main(String[] args) {

        KafkaConsumer<String, String> consumer = createKafkaConsumer();
        KafkaProducer<String, String> producer = createKafkaProducer();

        producer.initTransactions();

        try {

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(ofSeconds(60));
                logger.info("Polled {} records", records.count());

                Map<String, Integer> wordCountMap = StreamSupport.stream(records.spliterator(), false)
                        .flatMap(record -> Stream.of(record.value().split(" ")))
                        .map(word -> Tuple.of(word, 1))
                        .collect(Collectors.toMap(Tuple::getKey, Tuple::getValue, Integer::sum));

                producer.beginTransaction();

                wordCountMap.forEach((key, value) -> producer.send(new ProducerRecord<>(OUTPUT_TOPIC, key, value.toString())));

                // We need to calculate the offsets for each topic partition
                Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionedRecords = records.records(partition);
                    long offset = partitionedRecords.get(partitionedRecords.size() - 1).offset();
                    logger.info("Topic partition {} last offset {}", partition.partition(), offset);

                    // The offsets you commit are the offsets of the messages you want to read next
                    offsetsToCommit.put(partition, new OffsetAndMetadata(offset + 1));
                }

                // Without transactions, you normally use Consumer#commitSync() or Consumer#commitAsync() to commit consumer offsets.
                // But if you use these methods before you've produced with your producer, you will have committed offsets
                // before knowing whether the producer succeeded sending.

                // Producer#sendOffsetsToTransaction() sends the offsets to the transaction manager handling the transaction.
                // It will commit the offsets only if the entire transactions—consuming and producing—succeeds.
                producer.sendOffsetsToTransaction(offsetsToCommit, CONSUMER_GROUP_ID);


                producer.commitTransaction();

            }

        } catch (KafkaException e) {

            producer.abortTransaction();

        }

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing producer.");
            producer.close();
            consumer.close();
        }));

    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(INPUT_TOPIC));
        return consumer;
    }

    private static KafkaProducer<String, String> createKafkaProducer() {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, TRANSACTIONAL_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer(props);

    }

    static class Tuple {
        private String key;
        private Integer value;

        private Tuple(String key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public static Tuple of(String key, Integer value){
            return new Tuple(key,value);
        }

        public String getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }
    }
}
