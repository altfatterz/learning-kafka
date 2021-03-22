package com.example;

import com.example.model.Customer.CustomerOuterClass.Customer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

import static com.example.model.Customer.CustomerOuterClass.Customer.PhoneType.MOBILE;

public class ProducerApp {

    private static final String KAFKA_TOPIC = "demo-topic-protobuf";
    private static final Logger logger = LoggerFactory.getLogger(ProducerApp.class);

    public static void main(String[] args) throws InterruptedException {

        logger.info("Starting Java Protobuf producer.");

        final Properties settings = new Properties();
        settings.put(ProducerConfig.CLIENT_ID_CONFIG, "clientId1");
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");

        settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);

        settings.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        settings.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, List.of(MonitoringProducerInterceptor.class));

        final KafkaProducer<String, Customer> producer = new KafkaProducer<>(settings);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing producer.");
            producer.close();
        }));

        Customer customer = Customer.newBuilder()
                .setId(1)
                .setFirstName("John")
                .setLastName("Doe")
                .setEmail("johndoe@gmail.com")
                .addPhones(Customer.PhoneNumber.newBuilder()
                        .setType(MOBILE)
                        .setNumber("0761234678")
                        .build())
                .build();

        final ProducerRecord<String, Customer> record = new ProducerRecord<>(
                KAFKA_TOPIC, "111", customer);

        // send data asynchronously
        producer.send(record, (recordMetadata, exception) -> {
            if (exception == null) {
               logger.info("Partition: {}, Offset: {}, SerializedValueSize: {}", recordMetadata.partition(),
                       recordMetadata.offset(), recordMetadata.serializedValueSize());
            } else {
                exception.printStackTrace();
            }
        });


    }
}
