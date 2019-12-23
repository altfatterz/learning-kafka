package com.github.altfatterz;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaProducerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaProducerDemo.class);

    static final String BOOTSTRAP_SERVERS = "localhost:9092";
    static final String TOPIC = "demo-topic";

    public static void main(String[] args) {

        // producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // create a producer record
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "learning kafka");

        logger.info("send message asynchronously....");
        producer.send(record);

        logger.info("flushing and closing the producer");
        producer.close();
    }

}
