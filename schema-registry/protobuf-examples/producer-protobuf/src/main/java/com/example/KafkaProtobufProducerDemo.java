package com.example;

import com.example.model.Customer.CustomerOuterClass.Customer;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static com.example.model.Customer.CustomerOuterClass.Customer.PhoneType.MOBILE;

public class KafkaProtobufProducerDemo {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProtobufProducerDemo.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        final KafkaProducer<String, Customer> producer = new KafkaProducer<>(props);

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

        final ProducerRecord<String, Customer> record = new ProducerRecord<>(topic, "111", customer);

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
