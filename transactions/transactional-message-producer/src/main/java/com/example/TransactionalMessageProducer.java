package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.stream.Stream;

public class TransactionalMessageProducer {

    static final Logger logger = LoggerFactory.getLogger(TransactionalMessageProducer.class);

    static final String BOOTSTRAP_SERVERS = "localhost:19092";
    static final String TOPIC = "input-topic";

    private static final String DATA_MESSAGE_1 = "Put any space separated data here for count";
    private static final String DATA_MESSAGE_2 = "Output will contain count of every word in the message";


    public static void main(String[] args) {

        // producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "producer-1");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        producer.initTransactions();

        try {
            producer.beginTransaction();

            Stream.of(DATA_MESSAGE_1, DATA_MESSAGE_2)
                    .forEach(message -> producer.send(new ProducerRecord<>(TOPIC, null, message)));

            producer.commitTransaction();

        } catch (KafkaException e) {
            producer.abortTransaction();
        }

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing producer.");
            producer.close();
        }));

    }


}
